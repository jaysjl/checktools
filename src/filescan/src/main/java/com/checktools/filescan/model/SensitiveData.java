package com.checktools.filescan.model;

/**
 * 敏感数据模型类
 * 用于存储检测到的敏感数据信息，包括类型、内容和在可见字符中的位置
 */
public class SensitiveData {

    /** 敏感数据类型枚举 */
    public enum DataType {
        /** 身份证号 */
        ID_CARD("身份证号"),
        /** 手机号 */
        PHONE("手机号"),
        /** 邮箱地址 */
        EMAIL("邮箱");

        private final String displayName;

        DataType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** 敏感数据类型 */
    private DataType type;

    /** 敏感数据内容 */
    private String content;

    /** 在可见字符串中的起始位置 */
    private int startIndex;

    public SensitiveData() {
    }

    public SensitiveData(DataType type, String content, int startIndex) {
        this.type = type;
        this.content = content;
        this.startIndex = startIndex;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    @Override
    public String toString() {
        return "SensitiveData{" +
                "type=" + type.getDisplayName() +
                ", content='" + content + '\'' +
                ", startIndex=" + startIndex +
                '}';
    }
}
