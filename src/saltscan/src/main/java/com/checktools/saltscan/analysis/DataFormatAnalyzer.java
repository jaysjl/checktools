package com.checktools.saltscan.analysis;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

/**
 * 数据格式分析器 - 识别并还原数据格式
 */
public class DataFormatAnalyzer {

    /**
     * 分析数据格式
     * 检测优先级：HEX (偶数长度) > BASE64 > RAW
     */
    public DataFormatType analyzeFormat(String data) {
        return analyzeFormat(data, false);
    }

    /**
     * 分析数据格式 - 带有BLOB标志
     * 如果数据来自BLOB列（已被数据库驱动转换为HEX），需要先还原为原始字节，
     * 然后分析原始字节本身的格式（是否是HEX/Base64编码的数据）
     * @param data 数据（来自BLOB列时已被驱动转换为HEX字符串）
     * @param isFromBlobColumn 是否来自BLOB列（已转换为HEX）
     */
    public DataFormatType analyzeFormat(String data, boolean isFromBlobColumn) {
        if (data == null || data.isEmpty()) {
            return DataFormatType.RAW;
        }

        // 如果来自BLOB列，需要特殊处理：
        // 1. BLOB中存储的可能本身就是文本（如身份证号）或编码的字符串（如HEX字符串）
        // 2. 数据库驱动已将BLOB转换为HEX（如 "48656C6C6F" 表示 "Hello"）
        // 3. 需要先将这个HEX还原为原始字节，然后分析原始字节是什么格式
        if (isFromBlobColumn) {
            return analyzeFormatFromBlob(data);
        }

        if (isHexFormat(data)) {
            return DataFormatType.HEX;
        }

        if (isBase64Format(data)) {
            return DataFormatType.BASE64;
        }

        return DataFormatType.RAW;
    }

    /**
     * 分析来自BLOB列的数据格式
     * BLOB数据由数据库驱动转换为HEX字符串，需要：
     * 1. 先将HEX还原为原始字节
     * 2. 将原始字节作为字符串尝试识别其格式（是否是HEX/Base64/RAW）
     */
    private DataFormatType analyzeFormatFromBlob(String hexData) {
        try {
            // 将数据库驱动转换的HEX还原为原始字节
            byte[] originalBytes = decodeHex(hexData);
            
            // 将原始字节转换为字符串，尝试分析其格式
            String originalString = new String(originalBytes, StandardCharsets.UTF_8);
            
            // 检查原始字符串是否是HEX格式
            if (isHexFormat(originalString)) {
                return DataFormatType.HEX;
            }
            
            // 检查原始字符串是否是Base64格式
            if (isBase64Format(originalString)) {
                return DataFormatType.BASE64;
            }
            
            // 否则认为是RAW格式
            return DataFormatType.RAW;
        } catch (Exception e) {
            // 如果转换失败，认为是RAW格式
            return DataFormatType.RAW;
        }
    }

    /**
     * 检查是否为HEX格式
     * HEX格式特征：偶数长度，必须包含a-f或A-F字符（不能全是0-9）
     * 这样可以区分真正的HEX编码和纯数字数据
     */
    private boolean isHexFormat(String data) {
        // HEX格式必须是偶数长度
        if (data.length() % 2 != 0) {
            return false;
        }
        // 必须只包含0-9, a-f, A-F
        if (!data.matches("^[0-9a-fA-F]+$")) {
            return false;
        }
        // 关键：必须包含至少一个a-f或A-F字符（不能全是0-9）
        // 这样可以区分真正的HEX编码（如"48656c6c6f"）和纯数字数据（如"110101199003071515"）
        return data.matches(".*[a-fA-F]+.*");
    }

    /**
     * 检查是否为Base64格式
     * Base64格式特征：包含大写字母+小写字母、或包含+/等特殊字符、或以=结尾
     * 单纯的纯数字或纯字母或纯小写，通常不是Base64编码
     */
    private boolean isBase64Format(String data) {
        // Base64格式基础验证
        if (!data.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }
        
        // 对于纯数字字符串，不认为是Base64（如"13812345678"手机号）
        if (data.matches("^[0-9]+$")) {
            return false;
        }
        
        // 对于纯小写字母，不认为是Base64编码
        if (data.matches("^[a-z]+$")) {
            return false;
        }
        
        // 对于过短的字符串，不认为是Base64（通常Base64编码会产生相对较长的字符串）
        if (data.length() < 4) {
            return false;
        }
        
        // 检查是否包含大小写混合、或+/等特殊字符、或=结尾（这些是Base64的特征）
        boolean hasMixedCase = data.matches(".*[a-z].*") && data.matches(".*[A-Z].*");
        boolean hasSpecialChars = data.contains("+") || data.contains("/");
        boolean hasEqualSign = data.endsWith("=");
        
        if (!hasMixedCase && !hasSpecialChars && !hasEqualSign) {
            // 没有Base64的明显特征
            return false;
        }
        
        try {
            Base64.decodeBase64(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 还原数据为字节数组
     */
    public byte[] decode(String data, DataFormatType format) {
        switch (format) {
            case HEX:
                return decodeHex(data);
            case BASE64:
                return decodeBase64(data);
            case RAW:
            default:
                return data.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 解码HEX格式
     */
    private byte[] decodeHex(String hex) {
        try {
            return Hex.decodeHex(hex);
        } catch (Exception e) {
            return hex.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 从BLOB转换的HEX字符串解码为二进制数据
     * 用于处理从数据库BLOB列转换来的HEX字符串
     */
    public byte[] decodeHexFromBlob(String hexString) {
        try {
            return Hex.decodeHex(hexString);
        } catch (Exception e) {
            return hexString.getBytes(StandardCharsets.UTF_8);
        }
    }
    /**
     * 解码Base64格式
     */
    private byte[] decodeBase64(String base64) {
        try {
            return Base64.decodeBase64(base64);
        } catch (Exception e) {
            return base64.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 编码为HEX格式
     */
    public String encodeHex(byte[] data) {
        return Hex.encodeHexString(data);
    }

    /**
     * 编码为Base64格式
     */
    public String encodeBase64(byte[] data) {
        return Base64.encodeBase64String(data);
    }
}
