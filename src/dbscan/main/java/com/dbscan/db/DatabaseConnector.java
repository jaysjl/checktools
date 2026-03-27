package com.dbscan.db;

import com.dbscan.config.JdbcConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接器，管理数据库连接
 */
public class DatabaseConnector {
    private Connection connection;
    private JdbcConfig config;

    public DatabaseConnector(JdbcConfig config) {
        this.config = config;
    }

    /**
     * 建立数据库连接
     * @throws SQLException 连接失败异常
     * @throws ClassNotFoundException 数据库驱动未找到异常
     */
    public void connect() throws SQLException, ClassNotFoundException {
        loadDriver(config.getType());
        connection = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
    }

    /**
     * 加载对应数据库的 JDBC 驱动
     * @param databaseType 数据库类型
     * @throws ClassNotFoundException 驱动类未找到异常
     */
    private void loadDriver(String databaseType) throws ClassNotFoundException {
        String driverClass;
        switch (databaseType.toUpperCase()) {
            case "MYSQL":
                driverClass = "com.mysql.cj.jdbc.Driver";
                break;
            case "POSTGRESQL":
                driverClass = "org.postgresql.Driver";
                break;
            case "SQLSERVER":
                driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                break;
            case "ORACLE":
                driverClass = "oracle.jdbc.driver.OracleDriver";
                break;
            default:
                throw new ClassNotFoundException("不支持的数据库类型: " + databaseType);
        }
        Class.forName(driverClass);
    }

    /**
     * 获取当前数据库连接
     * @return 数据库连接
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 关闭数据库连接
     * @throws SQLException 关闭异常
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * 测试数据库连接
     * @return 连接成功返回 true，否则返回 false
     */
    public boolean testConnection() {
        try {
            connect();
            if (connection != null && !connection.isClosed()) {
                close();
                return true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("数据库连接测试失败: " + e.getMessage());
        }
        return false;
    }
}
