package com.dbscan.config;

import java.util.List;

/**
 * 扫描配置
 */
public class ScanConfig {
    private List<String> targets;     // 扫描目标表列表 [schema.]table
    private Integer limit;            // 采样行数，0 表示全量
    private List<ScanRule> rules;     // 扫描规则列表
    private Integer concurrency;      // 并发数

    public ScanConfig() {
    }

    public ScanConfig(List<String> targets, Integer limit, List<ScanRule> rules, Integer concurrency) {
        this.targets = targets;
        this.limit = limit;
        this.rules = rules;
        this.concurrency = concurrency;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public List<ScanRule> getRules() {
        return rules;
    }

    public void setRules(List<ScanRule> rules) {
        this.rules = rules;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }
}
