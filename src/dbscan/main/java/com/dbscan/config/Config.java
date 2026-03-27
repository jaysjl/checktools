package com.dbscan.config;

/**
 * 根配置类，对应 JSON 配置文件结构
 */
public class Config {
    private JdbcConfig jdbc;    // JDBC 配置
    private ScanConfig scan;    // 扫描配置
    private OutputConfig output; // 输出配置

    public Config() {
    }

    public Config(JdbcConfig jdbc, ScanConfig scan, OutputConfig output) {
        this.jdbc = jdbc;
        this.scan = scan;
        this.output = output;
    }

    public JdbcConfig getJdbc() {
        return jdbc;
    }

    public void setJdbc(JdbcConfig jdbc) {
        this.jdbc = jdbc;
    }

    public ScanConfig getScan() {
        return scan;
    }

    public void setScan(ScanConfig scan) {
        this.scan = scan;
    }

    public OutputConfig getOutput() {
        return output;
    }

    public void setOutput(OutputConfig output) {
        this.output = output;
    }
}
