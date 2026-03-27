package com.dbscan.config;

/**
 * JDBC 数据库配置
 */
public class JdbcConfig {
    private String type;      // MySQL, PostgreSQL, SQLServer, Oracle
    private String url;       // JDBC URL
    private String username;  // 数据库用户名
    private String password;  // 数据库密码
    private String ip;        // 数据库 IP 地址
    private Integer port;     // 数据库端口
    private String database;  // 数据库名称

    public JdbcConfig() {
    }

    public JdbcConfig(String type, String url, String username, String password) {
        this.type = type;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public JdbcConfig(String type, String url, String username, String password, String ip, Integer port, String database) {
        this.type = type;
        this.url = url;
        this.username = username;
        this.password = password;
        this.ip = ip;
        this.port = port;
        this.database = database;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
