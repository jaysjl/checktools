package com.dbscan.scan;

import com.dbscan.config.Config;
import com.dbscan.config.ScanRule;
import com.dbscan.db.DatabaseConnector;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据扫描器，执行敏感数据扫描
 */
public class DataScanner {
    private DatabaseConnector connector;
    private Config config;
    private List<ScanResult> results;

    public DataScanner(DatabaseConnector connector, Config config) {
        this.connector = connector;
        this.config = config;
        this.results = new ArrayList<>();
    }

    /**
     * 执行扫描
     * @throws SQLException 数据库查询异常
     */
    public void scan() throws SQLException {
        System.out.println("开始扫描敏感数据...");
        
        Connection connection = connector.getConnection();
        DatabaseMetaData metaData = connection.getMetaData();

        // 遍历扫描目标
        for (String target : config.getScan().getTargets()) {
            String[] parts = target.split("\\.");
            String schema = parts.length == 2 ? parts[0] : null;
            String table = parts.length == 2 ? parts[1] : parts[0];

            scanTable(connection, metaData, schema, table);
        }

        System.out.println("扫描完成，共发现 " + results.size() + " 处敏感数据");
    }

    /**
     * 获取数据库类型
     * @return 数据库类型
     */
    private String getDatabaseType() {
        return config.getJdbc().getType().toUpperCase();
    }

    /**
     * 扫描单个表
     * @param connection 数据库连接
     * @param metaData 数据库元数据
     * @param schema schema 名称（可为 null）
     * @param table 表名称
     * @throws SQLException 数据库查询异常
     */
    private void scanTable(Connection connection, DatabaseMetaData metaData, String schema, String table) throws SQLException {
        System.out.println("扫描表: " + (schema != null ? schema + "." : "") + table);

        String databaseType = getDatabaseType();
        String metaDataSchema = schema;
        
        // MySQL 中，schema 等价于 database，需要在连接字符串中处理
        // 其他数据库中 schema 直接用于 getColumns() 方法
        if ("MYSQL".equals(databaseType)) {
            // MySQL 使用 database 字段，schema 应该为 null，但需要在 SQL 查询中指定
            metaDataSchema = null;
        }

        // 获取表的列信息
        try (ResultSet columns = metaData.getColumns(null, metaDataSchema, table, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");

                // 只扫描字符类型列
                if (isStringType(columnType)) {
                    scanColumn(connection, schema, table, columnName);
                }
            }
        } catch (SQLException e) {
            System.err.println("获取表 " + table + " 的列信息失败: " + e.getMessage());
        }
    }

    /**
     * 扫描单个列
     * @param connection 数据库连接
     * @param schema schema 名称（可为 null）
     * @param table 表名称
     * @param column 列名称
     * @throws SQLException 数据库查询异常
     */
    private void scanColumn(Connection connection, String schema, String table, String column) throws SQLException {
        String schemaPrefix = schema != null ? schema + "." : "";
        String query = buildSelectQuery(schemaPrefix + table, column);

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            
            List<String> matchedSamples = new ArrayList<>();
            long extractCount = 0;
            long matchCount = 0;
            ScanRule matchedRule = null;
            Integer sampleLimit = config.getOutput().getSample();
            if (sampleLimit == null || sampleLimit <= 0) {
                sampleLimit = 5; // 默认采样5条
            }

            while (rs.next()) {
                extractCount++;
                String value = rs.getString(1);
                if (value != null) {
                    // 检查是否匹配任何规则
                    for (ScanRule rule : config.getScan().getRules()) {
                        Pattern pattern = Pattern.compile(rule.getRegex());
                        if (pattern.matcher(value).find()) {
                            matchCount++;
                            matchedRule = rule;
                            // 采样限制：只保存前 sampleLimit 个匹配的样本
                            if (matchedSamples.size() < sampleLimit) {
                                matchedSamples.add(value);
                            }
                            break;
                        }
                    }
                }
            }

            // 如果找到匹配项，生成扫描结果
            if (matchCount > 0 && matchedRule != null) {
                double matchRate = (matchCount * 100.0) / extractCount;
                
                ScanResult result = new ScanResult(
                        config.getJdbc().getIp(),
                        config.getJdbc().getPort(),
                        config.getJdbc().getType(),
                        config.getJdbc().getDatabase(),
                        schema,
                        table,
                        column,
                        extractCount,
                        matchedRule.getName(),
                        matchedRule.getDescription(),
                        matchRate,
                        String.join("; ", matchedSamples)
                );
                results.add(result);
                System.out.println("  找到敏感数据: " + column + " (" + matchedRule.getName() + ") - " + matchCount + " 条 (匹配率: " + String.format("%.2f%%", matchRate) + ")");
            }

            rs.close();
        } catch (SQLException e) {
            System.err.println("扫描列 " + column + " 失败: " + e.getMessage());
        }
    }

    /**
     * 构建 SELECT 查询语句
     * @param table 表名
     * @param column 列名
     * @return SQL 查询语句
     */
    private String buildSelectQuery(String table, String column) {
        Integer limit = config.getScan().getLimit();
        String databaseType = getDatabaseType();
        String baseQuery = "SELECT " + column + " FROM " + table;
        
        if (limit == null || limit == 0) {
            // 全量查询
            return baseQuery;
        } else {
            // 根据不同数据库类型生成兼容的 LIMIT 语句
            switch (databaseType) {
                case "ORACLE":
                    // Oracle 使用 ROWNUM 或 FETCH FIRST...ROWS ONLY
                    return baseQuery + " WHERE ROWNUM <= " + limit;
                case "SQLSERVER":
                    // SQL Server 使用 OFFSET...FETCH 或 TOP
                    return "SELECT TOP " + limit + " " + column + " FROM " + table;
                case "POSTGRESQL":
                case "MYSQL":
                default:
                    // PostgreSQL 和 MySQL 使用 LIMIT
                    return baseQuery + " LIMIT " + limit;
            }
        }
    }

    /**
     * 判断列类型是否为字符类型
     * @param columnType 列类型
     * @return 是字符类型返回 true，否则返回 false
     */
    private boolean isStringType(String columnType) {
        String type = columnType.toUpperCase();
        return type.contains("VARCHAR") || type.contains("CHAR") || type.contains("TEXT")
                || type.contains("STRING") || type.contains("CLOB");
    }

    /**
     * 获取扫描结果
     * @return 扫描结果列表
     */
    public List<ScanResult> getResults() {
        return results;
    }
}
