package com.dbscan.config;

/**
 * 输出配置
 */
public class OutputConfig {
    private String path;    // 报告输出路径
    private Integer sample; // 采样数量

    public OutputConfig() {
    }

    public OutputConfig(String path) {
        this.path = path;
        this.sample = 5; // 默认采样5条
    }

    public OutputConfig(String path, Integer sample) {
        this.path = path;
        this.sample = sample;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSample() {
        return sample;
    }

    public void setSample(Integer sample) {
        this.sample = sample;
    }
}
