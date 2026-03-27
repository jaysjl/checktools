package com.checktools.saltscan.analysis;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

/**
 * 数据格式分析器单元测试
 */
public class DataFormatAnalyzerTest {
    
    private DataFormatAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new DataFormatAnalyzer();
    }

    @Test
    public void testAnalyzeHexFormat() {
        String hexData = "48656C6C6F"; // "Hello" in hex
        DataFormatType type = analyzer.analyzeFormat(hexData);
        assertEquals(DataFormatType.HEX, type);
    }

    @Test
    public void testAnalyzeBase64Format() {
        String base64Data = "SGVsbG8gV29ybGQ="; // "Hello World" in base64
        DataFormatType type = analyzer.analyzeFormat(base64Data);
        assertEquals(DataFormatType.BASE64, type);
    }

    @Test
    public void testAnalyzeRawFormat() {
        String rawData = "Hello World!";
        DataFormatType type = analyzer.analyzeFormat(rawData);
        assertEquals(DataFormatType.RAW, type);
    }

    @Test
    public void testDecodeHex() {
        String hexData = "48656C6C6F";
        byte[] decoded = analyzer.decode(hexData, DataFormatType.HEX);
        String result = new String(decoded, StandardCharsets.UTF_8);
        assertEquals("Hello", result);
    }

    @Test
    public void testDecodeBase64() {
        String base64Data = "SGVsbG8gV29ybGQ=";
        byte[] decoded = analyzer.decode(base64Data, DataFormatType.BASE64);
        String result = new String(decoded, StandardCharsets.UTF_8);
        assertEquals("Hello World", result);
    }

    @Test
    public void testDecodeRaw() {
        String rawData = "Hello World";
        byte[] decoded = analyzer.decode(rawData, DataFormatType.RAW);
        String result = new String(decoded, StandardCharsets.UTF_8);
        assertEquals("Hello World", result);
    }

    @Test
    public void testEncodeHex() {
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        String encoded = analyzer.encodeHex(data);
        assertEquals("48656c6c6f", encoded);
    }

    @Test
    public void testEncodeBase64() {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        String encoded = analyzer.encodeBase64(data);
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
    }

    @Test
    public void testIsHexFormatOddLength() {
        String oddHex = "48656C6C6F1"; // 奇数长度
        DataFormatType type = analyzer.analyzeFormat(oddHex);
        assertNotEquals(DataFormatType.HEX, type);
    }

    @Test
    public void testIsHexFormatWithInvalidChars() {
        String invalidHex = "48656C6C6FGG"; // 包含无效字符
        DataFormatType type = analyzer.analyzeFormat(invalidHex);
        assertNotEquals(DataFormatType.HEX, type);
    }

    @Test
    public void testAnalyzeNullData() {
        DataFormatType type = analyzer.analyzeFormat(null);
        assertEquals(DataFormatType.RAW, type);
    }

    @Test
    public void testAnalyzeEmptyData() {
        DataFormatType type = analyzer.analyzeFormat("");
        assertEquals(DataFormatType.RAW, type);
    }
}
