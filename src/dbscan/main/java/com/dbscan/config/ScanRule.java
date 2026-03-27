package com.dbscan.config;

/**
 * 扫描规则配置
 */
public class ScanRule {
    private String name;           // 规则名称
    private String description;    // 规则描述
    private String regex;          // 正则表达式

    public ScanRule() {
    }

    public ScanRule(String name, String description, String regex) {
        this.name = name;
        this.description = description;
        this.regex = regex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }
}
