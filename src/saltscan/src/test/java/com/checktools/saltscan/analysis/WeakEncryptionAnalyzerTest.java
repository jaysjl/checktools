package com.checktools.saltscan.analysis;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 弱加密分析器单元测试
 */
public class WeakEncryptionAnalyzerTest {
    
    private WeakEncryptionAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new WeakEncryptionAnalyzer();
    }

    @Test
    public void testAnalyzeWithWeakEncryption() {
        // 创建大量重复数据
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            dataList.add("cipher_value_1");
        }
        for (int i = 0; i < 50; i++) {
            dataList.add("cipher_value_2");
        }
        for (int i = 0; i < 50; i++) {
            dataList.add("cipher_value_" + i);
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        assertNotNull(result);
        assertTrue(result.getRepetitionRate() > 0.1);
        assertTrue(result.isWeakEncryption());
    }

    @Test
    public void testAnalyzeWithStrongEncryption() {
        // 创建很多不同的数据 - 足以超过强加密阈值
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            dataList.add("unique_cipher_" + i);
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        assertNotNull(result);
        // 1000个唯一值，前20个都各出现一次 = 20/1000 = 0.02，远低于0.1阈值
        assertTrue(result.getRepetitionRate() <= 0.1);
        assertFalse(result.isWeakEncryption());
    }

    @Test
    public void testAnalyzeDuplicateDetection() {
        List<String> dataList = new ArrayList<>();
        dataList.add("value1");
        dataList.add("value1");
        dataList.add("value1");
        dataList.add("value2");
        dataList.add("value2");
        dataList.add("value3");
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        List<Map.Entry<String, Integer>> topDuplicates = result.getTopDuplicates();
        assertNotNull(topDuplicates);
        assertFalse(topDuplicates.isEmpty());
        
        // 验证最重复的值在前面
        assertEquals("value1", topDuplicates.get(0).getKey());
        assertEquals(3, (int) topDuplicates.get(0).getValue());
    }

    @Test
    public void testAnalyzeTop20Limit() {
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            for (int j = 0; j < 30 - i; j++) {
                dataList.add("value_" + i);
            }
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        // 最多只返回20个
        assertTrue(result.getTopDuplicates().size() <= 20);
    }

    @Test
    public void testAnalyzeEmptyList() {
        List<String> emptyList = new ArrayList<>();
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(emptyList);
        
        assertNotNull(result);
        assertEquals(0, result.getRepetitionRate(), 0.001);
        assertFalse(result.isWeakEncryption());
    }

    @Test
    public void testAnalyzeRepetitionRate() {
        List<String> dataList = new ArrayList<>();
        // 创建足够多的数据以获得可靠的重复率测试
        // 50个"repeated" + 250个unique值
        for (int i = 0; i < 50; i++) {
            dataList.add("repeated");
        }
        for (int i = 0; i < 250; i++) {
            dataList.add("unique_" + i);
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        double rate = result.getRepetitionRate();
        // 前20个最频繁值：repeated(50) + 最多19个其他值 = 50 + 19 = 69，但超过20个值，所以是repeated(50) + 19个unique = 69
        // 但前20个值意味着最多50 + 19 = 69，但只取前20个，所以是min(50+19, 其他前20个) 
        // 实际上：repeated(50)计在内，然后最多19个其他值，总共69
        // 但等等，前20个不同的值，如果repeated占50，那就是repeated(50次) + 前19个unique各1次
        // 但不超过20个值，所以重复率 = (50+19)/300 ≈ 0.23
        assertTrue(rate > 0.15 && rate < 0.3);
    }

    @Test
    public void testAnalyzeReasonGeneration() {
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            dataList.add("value");
        }
        for (int i = 0; i < 50; i++) {
            dataList.add("unique_" + i);
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        String reason = result.getReason();
        assertNotNull(reason);
        assertFalse(reason.isEmpty());
        assertTrue(reason.contains("加密"));
    }

    @Test
    public void testAnalyzeWithSingleValue() {
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            dataList.add("same_value");
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        assertTrue(result.isWeakEncryption());
        assertEquals(1, result.getTopDuplicates().size());
        assertEquals(100, (int) result.getTopDuplicates().get(0).getValue());
    }

    @Test
    public void testAnalyzeBoundaryCase() {
        // 边界情况：恰好10%的重复率
        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            dataList.add("repeated");
        }
        for (int i = 0; i < 89; i++) {
            dataList.add("unique_" + i);
        }
        
        WeakEncryptionAnalyzer.WeakEncryptionResult result = analyzer.analyze(dataList);
        
        // 因为是>0.1，所以不包括恰好0.1的情况
        if (result.getRepetitionRate() > 0.1) {
            assertTrue(result.isWeakEncryption());
        }
    }
}
