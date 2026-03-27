package com.checktools.saltscan.db;

import com.checktools.saltscan.config.ConfigManager;

import java.sql.*;
import java.util.*;

/**
 * 数据库连接和查询管理器
 * 支持MySQL、PostgreSQL、SQL Server、Oracle
 */
public class DatabaseConnector {
    
    private final ConfigManager configManager;
    private Connection connection;

    public DatabaseConnector(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 建立数据库连接
     */
    public void connect() throws SQLException {
        String dbType = configManager.getJdbcType();
        String url = configManager.getJdbcUrl();
        String username = configManager.getJdbcUsername();
        String password = configManager.getJdbcPassword();

        // 加载驱动程序
        try {
            loadDriver(dbType);
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] 数据库驱动加载失败: " + dbType);
            throw new SQLException("无法加载驱动程序: " + dbType, e);
        }

        this.connection = DriverManager.getConnection(url, username, password);
        System.out.println("[INFO] 数据库连接成功: " + url);
    }

    /**
     * 获取数据库类型
     */
    public String getDatabaseType() {
        return configManager.getJdbcType().toLowerCase();
    }

    /**
     * 验证数据库连接权限
     */
    public boolean validatePermission(String tableName) {
        try {
            String query = buildLimitQuery("SELECT 1 FROM " + tableName, 1);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery(query);
                System.out.println("[INFO] 表 " + tableName + " 权限验证成功");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[WARN] 表 " + tableName + " 权限验证失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据数据库类型构建LIMIT查询语句
     */
    private String buildLimitQuery(String baseQuery, int limit) {
        String dbType = getDatabaseType();
        switch(dbType) {
            case "mysql":
            case "postgresql":
                return baseQuery + " LIMIT " + limit;
            case "sqlserver":
                return "SELECT TOP " + limit + " * FROM (" + baseQuery + ") AS t";
            case "oracle":
                return "SELECT * FROM (" + baseQuery + ") WHERE ROWNUM <= " + limit;
            default:
                return baseQuery + " LIMIT " + limit;
        }
    }

    /**
     * 查询表中指定列的数据
     */
    public List<String> queryColumnData(String tableName, String columnName, int limit) throws SQLException {
        QueryResult result = queryColumnDataWithMetadata(tableName, columnName, limit);
        return result.getData();
    }

    /**
     * 查询表中指定列的数据，返回元数据信息
     */
    public QueryResult queryColumnDataWithMetadata(String tableName, String columnName, int limit) throws SQLException {
        List<String> results = new ArrayList<>();
        boolean fromBlobColumn = false;
        
        // 首先检查列的数据类型
        String columnType = getColumnType(tableName, columnName);
        String selectColumn = columnName;
        
        // 如果是 BLOB/LONGBLOB/BYTEA 类型，使用数据库特定函数转换为十六进制字符串
        // 这样可以正确处理二进制数据
        if (columnType != null && isBinaryType(columnType)) {
            selectColumn = buildHexFunction(columnName);
            fromBlobColumn = true;
            System.out.println("[DEBUG] 检测到列 " + columnName + " 的类型为 " + columnType + "，使用HEX转换");
        } else if (columnType == null && columnName.matches("^.*_encrypted$")) {
            // 如果列名以"_encrypted"结尾且无法检测类型，也尝试HEX转换
            // 这是为了处理name_encrypted和phone_encrypted这样的BLOB列
            // 但不会处理name_encrypted_hex这样的字符串列
            selectColumn = buildHexFunction(columnName);
            fromBlobColumn = true;
            System.out.println("[DEBUG] 列 " + columnName + " 可能为加密BLOB字段，使用HEX转换");
        }
        
        String query = limit > 0 
            ? buildLimitQuery(String.format("SELECT %s FROM %s", selectColumn, tableName), limit)
            : String.format("SELECT %s FROM %s", selectColumn, tableName);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String value = rs.getString(columnName);
                if (value != null) {
                    results.add(value);
                }
            }
        }
        
        System.out.println("[INFO] 从表 " + tableName + " 的列 " + columnName + " 查询到 " + results.size() + " 条记录");
        return new QueryResult(results, fromBlobColumn);
    }

    /**
     * 判断是否为二进制类型
     */
    private boolean isBinaryType(String columnType) {
        String upperType = columnType.toUpperCase();
        return upperType.contains("BLOB") || 
               upperType.contains("BYTEA") ||
               upperType.contains("BINARY") ||
               upperType.contains("VARBINARY") ||
               upperType.contains("RAW");
    }

    /**
     * 根据数据库类型生成HEX转换函数
     */
    private String buildHexFunction(String columnName) {
        String dbType = getDatabaseType();
        switch(dbType) {
            case "mysql":
                return "HEX(" + columnName + ") AS " + columnName;
            case "postgresql":
                return "encode(" + columnName + ", 'hex') AS " + columnName;
            case "sqlserver":
                return "CONVERT(VARCHAR(MAX), " + columnName + ", 2) AS " + columnName;
            case "oracle":
                return "RAWTOHEX(" + columnName + ") AS " + columnName;
            default:
                return "HEX(" + columnName + ") AS " + columnName;
        }
    }

    /**
     * 获取列的数据类型 - 支持MySQL/PostgreSQL/SQLServer/Oracle
     */
    private String getColumnType(String tableName, String columnName) throws SQLException {
        String dbType = getDatabaseType();
        String query = buildColumnTypeQuery(dbType, tableName, columnName);
        
        if (query == null) {
            return null;  // 不支持的数据库类型
        }
        
        try (java.sql.PreparedStatement stmt = connection.prepareStatement(query)) {
            // 根据数据库类型设置参数
            switch(dbType) {
                case "mysql":
                    stmt.setString(1, connection.getCatalog());  // 数据库名
                    stmt.setString(2, tableName);
                    stmt.setString(3, columnName);
                    break;
                case "postgresql":
                    stmt.setString(1, tableName);
                    stmt.setString(2, columnName);
                    break;
                case "sqlserver":
                    stmt.setString(1, tableName);
                    stmt.setString(2, columnName);
                    break;
                case "oracle":
                    stmt.setString(1, tableName.toUpperCase());
                    stmt.setString(2, columnName.toUpperCase());
                    break;
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);  // 第一列始终是数据类型
            }
        }
        
        return null;
    }

    /**
     * 根据数据库类型构建获取列类型的SQL查询
     */
    private String buildColumnTypeQuery(String dbType, String tableName, String columnName) {
        switch(dbType) {
            case "mysql":
                // MySQL INFORMATION_SCHEMA
                return "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                       "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            
            case "postgresql":
                // PostgreSQL information_schema
                return "SELECT data_type FROM information_schema.columns " +
                       "WHERE table_name = ? AND column_name = ?";
            
            case "sqlserver":
                // SQL Server sys.columns
                return "SELECT TYPE_NAME(user_type_id) FROM sys.columns " +
                       "WHERE OBJECT_ID = OBJECT_ID(?) AND name = ?";
            
            case "oracle":
                // Oracle user_tab_columns
                return "SELECT DATA_TYPE FROM user_tab_columns " +
                       "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
            
            default:
                return null;
        }
    }

    /**
     * 关闭连接
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("[INFO] 数据库连接已关闭");
        }
    }

    /**
     * 根据数据库类型加载驱动
     */
    private void loadDriver(String dbType) throws ClassNotFoundException {
        switch (dbType.toLowerCase()) {
            case "mysql":
                Class.forName("com.mysql.cj.jdbc.Driver");
                break;
            case "postgresql":
                Class.forName("org.postgresql.Driver");
                break;
            case "sqlserver":
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            case "oracle":
                Class.forName("oracle.jdbc.driver.OracleDriver");
                break;
            default:
                throw new ClassNotFoundException("不支持的数据库类型: " + dbType);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
