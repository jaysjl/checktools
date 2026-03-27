package com.checktools.saltscan.analysis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 伪加密分析器
 * 通过长度分布、归一化方差、字节频率分析判断是否为伪加密
 */
public class PseudoEncryptionAnalyzer {
    private final int dataLengthMin;
    private final int dataLengthIntervalMin;
    private final double dataVarianceMax;

    /**
     * 默认构造函数
     */
    public PseudoEncryptionAnalyzer() {
        this(16, 16, 1.0);
    }

    /**
     * 参数化构造函数
     */
    public PseudoEncryptionAnalyzer(int dataLengthMin, int dataLengthIntervalMin, double dataVarianceMax) {
        this.dataLengthMin = dataLengthMin;
        this.dataLengthIntervalMin = dataLengthIntervalMin;
        this.dataVarianceMax = dataVarianceMax;
    }

    /**
     * 分析数据是否为伪加密
     */
    public PseudoEncryptionResult analyze(List<byte[]> dataList) {
        PseudoEncryptionResult result = new PseudoEncryptionResult();
        
        // 1. 分析数据长度分布
        result.setLengthDistribution(analyzeLengthDistribution(dataList));
        
        // 2. 计算最小长度和最小长度间隔
        result.setMinLength(calculateMinLength(dataList));
        result.setMinLengthGap(calculateMinLengthGap(result.getLengthDistribution()));
        
        // 3. 分析字节频率分布和归一化方差
        result.setByteFrequency(analyzeByteFrequency(dataList));
        result.setNormalizedVariance(calculateNormalizedVariance(dataList));
        
        // 4. 判断伪加密
        result.setIsPseudoEncryption(isPseudoEncryption(result));
        result.setReason(generateReason(result));
        
        return result;
    }

    /**
     * 分析长度分布
     */
    private Map<Integer, Integer> analyzeLengthDistribution(List<byte[]> dataList) {
        return dataList.stream()
            .collect(Collectors.groupingBy(
                bytes -> bytes.length,
                Collectors.summingInt(e -> 1)
            ));
    }

    /**
     * 计算最小长度
     */
    private int calculateMinLength(List<byte[]> dataList) {
        return dataList.stream()
            .mapToInt(bytes -> bytes.length)
            .min()
            .orElse(0);
    }

    /**
     * 计算最小长度间隔
     */
    private int calculateMinLengthGap(Map<Integer, Integer> lengthDistribution) {
        if (lengthDistribution.size() <= 1) {
            return Integer.MAX_VALUE;
        }
        
        List<Integer> lengths = new ArrayList<>(lengthDistribution.keySet());
        Collections.sort(lengths);
        
        int minGap = Integer.MAX_VALUE;
        for (int i = 1; i < lengths.size(); i++) {
            int gap = lengths.get(i) - lengths.get(i - 1);
            if (gap > 0) {
                minGap = Math.min(minGap, gap);
            }
        }
        
        return minGap == Integer.MAX_VALUE ? 0 : minGap;
    }

    /**
     * 计算归一化方差
     * 对256个字节频率计算方差，然后除以均值的平方
     */
    private double calculateNormalizedVariance(List<byte[]> dataList) {
        if (dataList.isEmpty()) {
            return 0;
        }
        
        long totalBytes = dataList.stream().mapToLong(bytes -> bytes.length).sum();
        if (totalBytes == 0) return 0;
        
        // 计算256个字节的频率
        double[] frequencies = new double[256];
        for (byte[] data : dataList) {
            for (byte b : data) {
                frequencies[b & 0xFF]++;
            }
        }
        
        // 归一化频率
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = frequencies[i] / totalBytes;
        }
        
        // 计算均值
        double mean = 0;
        for (double freq : frequencies) {
            mean += freq;
        }
        mean /= frequencies.length;
        
        // 计算方差
        double variance = 0;
        for (double freq : frequencies) {
            variance += Math.pow(freq - mean, 2);
        }
        variance /= frequencies.length;
        
        // 计算归一化方差（方差除以均值的平方）
        if (mean > 0) {
            return variance / (mean * mean);
        }
        
        return 0;
    }

    /**
     * 分析字节频率分布
     */
    private Map<Integer, Double> analyzeByteFrequency(List<byte[]> dataList) {
        long totalBytes = dataList.stream().mapToLong(bytes -> bytes.length).sum();
        if (totalBytes == 0) return new HashMap<>();
        
        int[] frequency = new int[256];
        for (byte[] data : dataList) {
            for (byte b : data) {
                frequency[b & 0xFF]++;
            }
        }
        
        Map<Integer, Double> result = new HashMap<>();
        for (int i = 0; i < frequency.length; i++) {
            if (frequency[i] > 0) {
                result.put(i, (double) frequency[i] / totalBytes);
            }
        }
        
        return result;
    }

    /**
     * 判断是否为伪加密
     * 判断标准：
     * - 最小长度 < dataLengthMin OR 最小长度间隔 < dataLengthIntervalMin OR 归一化方差 >= dataVarianceMax 则为伪加密
     * - 否则未检测到伪加密
     */
    private boolean isPseudoEncryption(PseudoEncryptionResult result) {
        int minLength = result.getMinLength();
        int minLengthGap = result.getMinLengthGap();
        double normalizedVariance = result.getNormalizedVariance();
        
        // 任何一个条件满足就是伪加密
        return minLength < dataLengthMin || minLengthGap < dataLengthIntervalMin || normalizedVariance >= dataVarianceMax;
    }

    /**
     * 生成分析原因
     */
    private String generateReason(PseudoEncryptionResult result) {
        StringBuilder reason = new StringBuilder();
        
        int minLength = result.getMinLength();
        int minLengthGap = result.getMinLengthGap();
        double normalizedVariance = result.getNormalizedVariance();
        
        List<String> reasons = new ArrayList<>();
        
        if (minLength < dataLengthMin) {
            reasons.add("最小长度为 " + minLength + "，长度偏低");
        } else if (minLength >= dataLengthMin) {
            reasons.add("最小长度为 " + minLength + "，长度正常");
        }
        
        if (minLengthGap == Integer.MAX_VALUE) {
            // 所有长度都相同
            reasons.add("所有长度都为 " + minLength + "，长度间隔正常");
        } else if (minLengthGap < dataLengthIntervalMin) {
            reasons.add("最小长度间隔为 " + minLengthGap + "，长度间隔偏低");
        } else if (minLengthGap >= dataLengthIntervalMin) {
            reasons.add("最小长度间隔为 " + minLengthGap + "，长度间隔正常");
        }
        
        if (normalizedVariance >= 0.1) {
            reasons.add("归一化方差为 " + String.format("%.4f", normalizedVariance) + "，归一化方差较大");
        } else {
            reasons.add("归一化方差为 " + String.format("%.4f", normalizedVariance) + "，归一化方差较小");
        }
        
        reason.append(String.join("；", reasons));
        
        if (result.isPseudoEncryption()) {
            reason.append("。");
        }
        
        return reason.toString();
    }

    /**
     * 伪加密分析结果
     */
    public static class PseudoEncryptionResult {
        private Map<Integer, Integer> lengthDistribution;
        private int minLength;
        private int minLengthGap;
        private Map<Integer, Double> byteFrequency;
        private double normalizedVariance;
        private boolean isPseudoEncryption;
        private String reason;
        private List<String> rawData;

        public Map<Integer, Integer> getLengthDistribution() {
            return lengthDistribution;
        }

        public void setLengthDistribution(Map<Integer, Integer> lengthDistribution) {
            this.lengthDistribution = lengthDistribution;
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMinLengthGap() {
            return minLengthGap;
        }

        public void setMinLengthGap(int minLengthGap) {
            this.minLengthGap = minLengthGap;
        }

        public Map<Integer, Double> getByteFrequency() {
            return byteFrequency;
        }

        public void setByteFrequency(Map<Integer, Double> byteFrequency) {
            this.byteFrequency = byteFrequency;
        }

        public double getNormalizedVariance() {
            return normalizedVariance;
        }

        public void setNormalizedVariance(double normalizedVariance) {
            this.normalizedVariance = normalizedVariance;
        }

        public boolean isPseudoEncryption() {
            return isPseudoEncryption;
        }

        public void setIsPseudoEncryption(boolean pseudoEncryption) {
            isPseudoEncryption = pseudoEncryption;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public List<String> getRawData() {
            return rawData;
        }

        public void setRawData(List<String> rawData) {
            this.rawData = rawData;
        }
    }
}
