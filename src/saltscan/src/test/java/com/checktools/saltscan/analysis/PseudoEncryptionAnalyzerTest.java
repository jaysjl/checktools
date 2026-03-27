package com.checktools.saltscan.analysis;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 伪加密分析器单元测试
 */
public class PseudoEncryptionAnalyzerTest {
    
    private PseudoEncryptionAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new PseudoEncryptionAnalyzer();
    }

    @Test
    public void testAnalyzeWithPseudoEncryption() {
        // 创建伪加密数据（长度不均匀、方差大）
        List<byte[]> dataList = new ArrayList<>();
        dataList.add("ABC".getBytes());
        dataList.add("ABC".getBytes());
        dataList.add("DEF".getBytes());
        dataList.add("DEF".getBytes());
        dataList.add("GHI".getBytes());
        
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(dataList);
        
        assertNotNull(result);
        assertNotNull(result.getLengthDistribution());
        assertTrue(result.getNormalizedVariance() >= 0);
    }

    @Test
    public void testAnalyzeLengthDistribution() {
        List<byte[]> dataList = new ArrayList<>();
        dataList.add("A".getBytes());
        dataList.add("AB".getBytes());
        dataList.add("ABC".getBytes());
        dataList.add("ABCD".getBytes());
        
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(dataList);
        
        Map<Integer, Integer> distribution = result.getLengthDistribution();
        assertEquals(4, distribution.size());
        assertEquals(1, (int) distribution.get(1));
        assertEquals(1, (int) distribution.get(2));
        assertEquals(1, (int) distribution.get(3));
        assertEquals(1, (int) distribution.get(4));
    }

    @Test
    public void testAnalyzeWithUniformData() {
        // 创建方差较小的数据（接近加密数据）
        List<byte[]> dataList = new ArrayList<>();
        byte[] data1 = {(byte) 0xFF, (byte) 0x82, (byte) 0x45, (byte) 0xAB};
        byte[] data2 = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};
        byte[] data3 = {(byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};
        
        dataList.add(data1);
        dataList.add(data2);
        dataList.add(data3);
        
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(dataList);
        
        assertNotNull(result);
        assertTrue(result.getNormalizedVariance() > 0);
    }

    @Test
    public void testAnalyzeEmptyList() {
        List<byte[]> emptyList = new ArrayList<>();
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(emptyList);
        
        assertNotNull(result);
        assertEquals(0, result.getNormalizedVariance(), 0.001);
    }

    @Test
    public void testAnalyzeWithSingleByte() {
        List<byte[]> dataList = new ArrayList<>();
        dataList.add(new byte[]{(byte) 0x41}); // 'A'
        dataList.add(new byte[]{(byte) 0x42}); // 'B'
        dataList.add(new byte[]{(byte) 0x43}); // 'C'
        
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(dataList);
        
        assertNotNull(result);
        assertEquals(1, result.getLengthDistribution().size()); // 所有数据长度都是1
    }

    @Test
    public void testByteFrequencyDistribution() {
        List<byte[]> dataList = new ArrayList<>();
        // 使用相同字节频率，创建均匀分布
        dataList.add("AABBCC".getBytes());
        dataList.add("AABBCC".getBytes());
        
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(dataList);
        
        Map<Integer, Double> frequency = result.getByteFrequency();
        assertNotNull(frequency);
        assertFalse(frequency.isEmpty());
    }

    @Test
    public void testReasonGeneration() {
        List<byte[]> dataList = new ArrayList<>();
        dataList.add("ABC".getBytes());
        dataList.add("DEF".getBytes());
        
        PseudoEncryptionAnalyzer.PseudoEncryptionResult result = analyzer.analyze(dataList);
        
        assertNotNull(result.getReason());
        assertFalse(result.getReason().isEmpty());
        // 新的reason包含长度和方差信息，而不是加密相关文字
        assertTrue(result.getReason().contains("最小") || result.getReason().contains("方差"));
    }
}
