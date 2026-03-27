package com.checktools.filescan.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 分析结果模型类
 * 用于存储文件分析的完整结果，包括文件信息、可见字符和敏感数据列表
 */
public class AnalysisResult {

    /** 被分析的文件路径 */
    private String filePath;

    /** 文件大小（字节） */
    private long fileSize;

    /** 提取到的可见字符 */
    private String visibleChars;

    /** 可见字符数量 */
    private int visibleCharCount;

    /** 检测到的敏感数据列表 */
    private List<SensitiveData> sensitiveDataList;

    /** 分析是否完成（是否达到2000个可见字符上限） */
    private boolean reachedLimit;

    public AnalysisResult() {
        this.sensitiveDataList = new ArrayList<>();
    }

    public AnalysisResult(String filePath) {
        this.filePath = filePath;
        this.sensitiveDataList = new ArrayList<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getVisibleChars() {
        return visibleChars;
    }

    public void setVisibleChars(String visibleChars) {
        this.visibleChars = visibleChars;
    }

    public int getVisibleCharCount() {
        return visibleCharCount;
    }

    public void setVisibleCharCount(int visibleCharCount) {
        this.visibleCharCount = visibleCharCount;
    }

    public List<SensitiveData> getSensitiveDataList() {
        return sensitiveDataList;
    }

    public void setSensitiveDataList(List<SensitiveData> sensitiveDataList) {
        this.sensitiveDataList = sensitiveDataList;
    }

    public boolean isReachedLimit() {
        return reachedLimit;
    }

    public void setReachedLimit(boolean reachedLimit) {
        this.reachedLimit = reachedLimit;
    }

    /**
     * 添加一条敏感数据记录
     */
    public void addSensitiveData(SensitiveData data) {
        this.sensitiveDataList.add(data);
    }

    /**
     * 判断是否包含敏感数据
     */
    public boolean hasSensitiveData() {
        return !sensitiveDataList.isEmpty();
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", visibleCharCount=" + visibleCharCount +
                ", reachedLimit=" + reachedLimit +
                ", sensitiveDataCount=" + sensitiveDataList.size() +
                '}';
    }
}
