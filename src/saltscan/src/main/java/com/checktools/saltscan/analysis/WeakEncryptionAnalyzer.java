package com.checktools.saltscan.analysis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 弱加密分析器
 * 通过检测重复值来判断是否为弱加密（未加盐）
 */
public class WeakEncryptionAnalyzer {
    private final int repetitionCountMax;

    /**
     * 默认构造函数
     */
    public WeakEncryptionAnalyzer() {
        this(5);
    }

    /**
     * 参数化构造函数
     */
    public WeakEncryptionAnalyzer(int repetitionCountMax) {
        this.repetitionCountMax = repetitionCountMax;
    }

    /**
     * 分析数据是否为弱加密
     */
    public WeakEncryptionResult analyze(List<String> dataList) {
        WeakEncryptionResult result = new WeakEncryptionResult();
        
        // 1. 统计重复值
        Map<String, Integer> duplicateCount = calculateDuplicates(dataList);
        
        // 2. 提取前20个重复最多的值
        List<Map.Entry<String, Integer>> topDuplicates = duplicateCount.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(20)
            .collect(Collectors.toList());
        
        result.setTopDuplicates(topDuplicates);
        
        // 3. 计算重复率
        double repetitionRate = calculateRepetitionRate(dataList, topDuplicates);
        result.setRepetitionRate(repetitionRate);
        
        // 4. 获取最高重复值
        int maxRepetitionCount = topDuplicates.isEmpty() ? 0 : topDuplicates.get(0).getValue();
        result.setMaxRepetitionCount(maxRepetitionCount);
        
        // 5. 判断是否为弱加密
        result.setIsWeakEncryption(isWeakEncryption(topDuplicates));
        result.setReason(generateReason(topDuplicates, maxRepetitionCount));
        
        return result;
    }

    /**
     * 统计重复值
     */
    private Map<String, Integer> calculateDuplicates(List<String> dataList) {
        return dataList.stream()
            .collect(Collectors.groupingBy(
                str -> str,
                Collectors.summingInt(e -> 1)
            ));
    }

    /**
     * 计算重复率
     */
    private double calculateRepetitionRate(List<String> dataList, 
                                          List<Map.Entry<String, Integer>> topDuplicates) {
        if (dataList.isEmpty()) return 0;
        
        int totalRepeat = topDuplicates.stream()
            .mapToInt(Map.Entry::getValue)
            .sum();
        
        return (double) totalRepeat / dataList.size();
    }

    /**
     * 判断是否为弱加密
     * 如果最高重复值大于阈值，认为是弱加密
     */
    private boolean isWeakEncryption(List<Map.Entry<String, Integer>> topDuplicates) {
        if (topDuplicates.isEmpty()) {
            return false;
        }
        int maxRepetitionCount = topDuplicates.get(0).getValue();
        return maxRepetitionCount > repetitionCountMax;
    }

    /**
     * 生成分析原因
     */
    private String generateReason(List<Map.Entry<String, Integer>> topDuplicates, int maxRepetitionCount) {
        StringBuilder reason = new StringBuilder();
        
        if (maxRepetitionCount > repetitionCountMax) {
            reason.append("该数据很可能是弱加密。");
            reason.append(String.format("最高重复值达到%d次，", maxRepetitionCount));
            reason.append("表明相同的明文产生了相同的密文，说明未加盐或加密算法存在问题。");
        } else {
            reason.append("该数据看起来不是弱加密。");
            reason.append(String.format("最高重复值仅为%d次，", maxRepetitionCount));
            reason.append("数据分布相对均匀，可能加盐或使用了安全的加密算法。");
        }
        
        return reason.toString();
    }

    /**
     * 弱加密分析结果
     */
    public static class WeakEncryptionResult {
        private List<Map.Entry<String, Integer>> topDuplicates;
        private double repetitionRate;
        private int maxRepetitionCount;
        private boolean isWeakEncryption;
        private String reason;

        public List<Map.Entry<String, Integer>> getTopDuplicates() {
            return topDuplicates;
        }

        public void setTopDuplicates(List<Map.Entry<String, Integer>> topDuplicates) {
            this.topDuplicates = topDuplicates;
        }

        public double getRepetitionRate() {
            return repetitionRate;
        }

        public void setRepetitionRate(double repetitionRate) {
            this.repetitionRate = repetitionRate;
        }

        public int getMaxRepetitionCount() {
            return maxRepetitionCount;
        }

        public void setMaxRepetitionCount(int maxRepetitionCount) {
            this.maxRepetitionCount = maxRepetitionCount;
        }

        public boolean isWeakEncryption() {
            return isWeakEncryption;
        }

        public void setIsWeakEncryption(boolean weakEncryption) {
            isWeakEncryption = weakEncryption;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
