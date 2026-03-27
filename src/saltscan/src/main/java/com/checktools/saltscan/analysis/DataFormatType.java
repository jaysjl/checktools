package com.checktools.saltscan.analysis;

/**
 * 数据格式类型定义
 */
public enum DataFormatType {
    HEX("hex"),
    BASE64("base64"),
    RAW("raw");

    private final String value;

    DataFormatType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataFormatType fromValue(String value) {
        for (DataFormatType type : DataFormatType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return RAW;
    }
}
