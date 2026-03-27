package com.dbscan.scan;

/**
 * 扫描结果记录
 */
public class ScanResult {
    private String ip;               // IP 地址
    private Integer port;            // 端口
    private String databaseType;     // 数据库类型
    private String database;         // 库名
    private String schema;           // 模式名
    private String table;            // 表名
    private String column;           // 列名
    private Long extractCount;       // 抽取总数（scan.limit）
    private String ruleName;         // 规则名称
    private String ruleDescription;  // 规则描述
    private Double matchRate;        // 匹配率（百分比）
    private String samples;          // 采样（样例数据，多个用;分隔）

    public ScanResult() {
    }

    public ScanResult(String ip, Integer port, String databaseType, String database, String schema, 
                      String table, String column, Long extractCount, String ruleName, 
                      String ruleDescription, Double matchRate, String samples) {
        this.ip = ip;
        this.port = port;
        this.databaseType = databaseType;
        this.database = database;
        this.schema = schema;
        this.table = table;
        this.column = column;
        this.extractCount = extractCount;
        this.ruleName = ruleName;
        this.ruleDescription = ruleDescription;
        this.matchRate = matchRate;
        this.samples = samples;
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

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Long getExtractCount() {
        return extractCount;
    }

    public void setExtractCount(Long extractCount) {
        this.extractCount = extractCount;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    public Double getMatchRate() {
        return matchRate;
    }

    public void setMatchRate(Double matchRate) {
        this.matchRate = matchRate;
    }

    public String getSamples() {
        return samples;
    }

    public void setSamples(String samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", databaseType='" + databaseType + '\'' +
                ", database='" + database + '\'' +
                ", schema='" + schema + '\'' +
                ", table='" + table + '\'' +
                ", column='" + column + '\'' +
                ", extractCount=" + extractCount +
                ", ruleName='" + ruleName + '\'' +
                ", ruleDescription='" + ruleDescription + '\'' +
                ", matchRate=" + matchRate +
                ", samples='" + samples + '\'' +
                '}';
    }
}
