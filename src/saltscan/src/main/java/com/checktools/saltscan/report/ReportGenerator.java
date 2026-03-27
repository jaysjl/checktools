package com.checktools.saltscan.report;

import com.checktools.saltscan.analysis.DataFormatType;
import com.checktools.saltscan.analysis.PseudoEncryptionAnalyzer;
import com.checktools.saltscan.analysis.WeakEncryptionAnalyzer;
import com.checktools.saltscan.config.ConfigManager;
import com.checktools.saltscan.db.DatabaseConnector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HTML报告生成器 - 支持多列多表的扫描报告
 */
public class ReportGenerator {
    private final DatabaseConnector connector;
    private final ConfigManager configManager;
    private final List<ColumnAnalysisData> analysisDataList = new ArrayList<>();

    // 用于存储单列的分析数据
    private static class ColumnAnalysisData {
        String tableName;
        String columnName;
        DataFormatType formatType;
        PseudoEncryptionAnalyzer.PseudoEncryptionResult pseudoResult;
        WeakEncryptionAnalyzer.WeakEncryptionResult weakResult;
        List<byte[]> decodedData;
        List<String> rawData;

        ColumnAnalysisData(String tableName, String columnName, DataFormatType formatType,
                          PseudoEncryptionAnalyzer.PseudoEncryptionResult pseudoResult,
                          WeakEncryptionAnalyzer.WeakEncryptionResult weakResult,
                          List<byte[]> decodedData, List<String> rawData) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.formatType = formatType;
            this.pseudoResult = pseudoResult;
            this.weakResult = weakResult;
            this.decodedData = decodedData;
            this.rawData = rawData;
        }
    }

    public ReportGenerator(DatabaseConnector connector, ConfigManager configManager) {
        this.connector = connector;
        this.configManager = configManager;
    }

    /**
     * 添加单列的分析结果
     */
    public void addColumnAnalysis(String tableName, String columnName, DataFormatType formatType,
                                 PseudoEncryptionAnalyzer.PseudoEncryptionResult pseudoResult,
                                 WeakEncryptionAnalyzer.WeakEncryptionResult weakResult,
                                 List<byte[]> decodedData, List<String> rawData) {
        ColumnAnalysisData data = new ColumnAnalysisData(tableName, columnName, formatType, pseudoResult, weakResult, decodedData, rawData);
        analysisDataList.add(data);
    }

    /**
     * 生成HTML报告
     */
    public void generateReport(String outputPath) throws IOException {
        Document doc = createDocument();
        
        if (analysisDataList.isEmpty()) {
            doc.body().appendElement("p").text("没有扫描数据");
        } else {
            addHeader(doc);
            addScanSummary(doc);
            
            // 为每列生成分析报告
            for (ColumnAnalysisData data : analysisDataList) {
                addColumnReport(doc, data);
            }
        }
        
        addFooter(doc);
        addIntegrityCheck(doc);
        Files.write(Paths.get(outputPath), doc.outerHtml().getBytes(StandardCharsets.UTF_8));
    }

    private Document createDocument() {
        Document doc = Jsoup.parse("");
        doc.appendElement("html");
        doc.head().appendElement("meta").attr("charset", "UTF-8");
        doc.head().appendElement("title").text("密文数据强度分析报告");
        addStyles(doc);
        return doc;
    }

    private void addStyles(Document doc) {
        Element style = doc.head().appendElement("style");
        String cssContent = "* { margin: 0; padding: 0; } " +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; " +
                "line-height: 1.6; color: #333; background-color: #f5f5f5; padding: 20px; } " +
                ".container { max-width: 1400px; margin: 0 auto; background-color: white; " +
                "padding: 30px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); } " +
                "h1 { color: #1a73e8; border-bottom: 3px solid #1a73e8; padding-bottom: 10px; margin: 20px 0; } " +
                "h2 { color: #34a853; margin-top: 30px; padding-bottom: 5px; border-bottom: 1px solid #ddd; } " +
                "h3 { color: #4285f4; margin-top: 20px; } " +
                "h4 { color: #555; margin-top: 15px; } " +
                ".summary-box { background-color: #f0f7ff; border-left: 4px solid #1a73e8; padding: 15px; " +
                "margin: 15px 0; border-radius: 4px; } " +
                ".result-box { padding: 15px; margin: 10px 0; border-radius: 4px; border-left: 4px solid; } " +
                ".result-box.positive { border-color: #ea4335; background-color: #fff4f3; } " +
                ".result-box.negative { border-color: #34a853; background-color: #f1f8f5; } " +
                ".result-box.warning { border-color: #fbbc04; background-color: #fff8e1; } " +
                "table { width: 100%; border-collapse: collapse; margin: 15px 0; font-size: 12px; } " +
                "th, td { padding: 8px; text-align: center; border: 1px solid #ddd; } " +
                "th { background-color: #f0f0f0; font-weight: bold; } " +
                "tr:hover { background-color: #f9f9f9; } " +
                ".heatmap-table { margin: 20px 0; } " +
                ".heatmap-table tbody th { width: 45px; min-width: 45px; max-width: 45px; } " +
                ".heatmap-table td { width: 40px; height: 40px; font-size: 11px; } " +
                ".heat-0 { background-color: #cccccc; } " +
                ".heat-blue { background-color: #4285f4; color: white; } " +
                ".heat-green { background-color: #34a853; color: white; } " +
                ".heat-yellow { background-color: #fbbc04; } " +
                ".heat-orange { background-color: #fa8072; color: white; } " +
                ".heat-red { background-color: #ea4335; color: white; } " +
                ".footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #ddd; " +
                "text-align: center; color: #999; font-size: 12px; } " +
                ".chart { margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 4px; } " +
                "small { color: #666; font-size: 12px; }";
        style.text(cssContent);
    }

    private void addHeader(Document doc) {
        Element body = doc.body();
        Element container = body.appendElement("div").addClass("container");
        container.appendElement("h1").text("密文数据强度分析报告");
        
        Element info = container.appendElement("div").addClass("summary-box");
        info.appendElement("p").html("<strong>扫描时间:</strong> " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")));
        info.appendElement("p").html("<strong>数据库类型:</strong> " + configManager.getJdbcType());
        info.appendElement("p").html("<strong>数据库连接:</strong> " + configManager.getJdbcUrl());
        info.appendElement("p").html("<strong>数据库名称:</strong> " + configManager.getJdbcDatabase());
    }

    private void addScanSummary(Document doc) {
        Element container = doc.select(".container").first();
        container.appendElement("h2").text("检测详情");
        
        Element detailBox = container.appendElement("div").addClass("summary-box");
        detailBox.appendElement("p").html("<strong>检测时间:</strong> " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")));
        
        Element contentList = detailBox.appendElement("p").html("<strong>检测内容:</strong>");
        Element contentDiv = detailBox.appendElement("div");
        
        int index = 1;
        for (ColumnAnalysisData data : analysisDataList) {
            String formatType = getFormatTypeDisplay(data.formatType);
            String pseudoStatus = data.pseudoResult.isPseudoEncryption() ? "⚠️ 检测到伪加密" : "✅ 未检测到伪加密";
            String weakStatus = data.weakResult.isWeakEncryption() ? "⚠️ 检测到弱加密" : "✅ 未检测到弱加密";
            String summary = String.format("%d. %s.%s （%s、%s、%s）", 
                    index, data.tableName, data.columnName, formatType, pseudoStatus, weakStatus);
            contentDiv.appendElement("p").html("&nbsp;&nbsp;" + summary);
            index++;
        }
    }

    private String getFormatTypeDisplay(DataFormatType formatType) {
        switch (formatType) {
            case HEX:
                return "十六进制编码";
            case BASE64:
                return "Base64编码";
            default:
                return "未检测到编码";
        }
    }

    private void addColumnReport(Document doc, ColumnAnalysisData data) {
        Element container = doc.select(".container").first();
        
        // 列标题 + 折叠按钮
        String collapseId = "collapse-" + data.tableName + "-" + data.columnName;
        String buttonId = "button-" + data.tableName + "-" + data.columnName;
        Element titleDiv = container.appendElement("div").attr("style", "display: flex; align-items: center;");
        titleDiv.appendElement("h2").attr("style", "margin: 0; flex: 1;").text(data.tableName + "." + data.columnName);
        String onClickScript = "var btn = document.getElementById('" + buttonId + "'); " +
                "var div = document.getElementById('" + collapseId + "'); " +
                "div.style.display = div.style.display === 'none' ? 'block' : 'none'; " +
                "btn.textContent = div.style.display === 'none' ? '展开详情' : '折叠详情';";
        titleDiv.appendElement("button")
                .attr("id", buttonId)
                .attr("onclick", onClickScript)
                .attr("style", "padding: 5px 10px; cursor: pointer; background: #f0f0f0; border: 1px solid #ccc; border-radius: 4px;")
                .text("展开详情");
        
        // 可折叠的详情容器（默认隐藏）
        Element detailDiv = container.appendElement("div")
                .attr("id", collapseId)
                .attr("style", "display: none; padding: 10px; margin: 10px 0;");
        
        // 总体报告
        addOverallReport(detailDiv, data);
        
        // 数据格式分析
        addDataFormatAnalysis(detailDiv, data);
        
        // 伪加密分析
        addPseudoEncryptionAnalysis(detailDiv, data);
        
        // 弱加密分析
        addWeakEncryptionAnalysis(detailDiv, data);
    }

    private void addOverallReport(Element container, ColumnAnalysisData data) {
        container.appendElement("h3").text("总体报告");
        
        Element box = container.appendElement("div").addClass("summary-box");
        
        // 编码方式
        String encodingDesc = "未检测到编码";
        if (data.formatType == DataFormatType.HEX) {
            encodingDesc = "十六进制编码";
        } else if (data.formatType == DataFormatType.BASE64) {
            encodingDesc = "Base64编码";
        }
        box.appendElement("p").html("<strong>编码方式:</strong> " + encodingDesc);
        
        // 伪加密
        String pseudoDesc = data.pseudoResult.isPseudoEncryption() ? "⚠️ 检测到伪加密" : "✅ 未检测到伪加密";
        box.appendElement("p").html("<strong>伪加密检测:</strong> " + pseudoDesc);
        
        // 弱加密
        String weakDesc = data.weakResult.isWeakEncryption() ? "⚠️ 检测到弱加密" : "✅ 未检测到弱加密";
        box.appendElement("p").html("<strong>弱加密检测:</strong> " + weakDesc);
        
        // 备注
        Element note = box.appendElement("p");
        note.html("<small>(根据检测算法，明文数据也可能被检测成\"伪加密\"或\"弱加密\")</small>");
    }

    private void addDataFormatAnalysis(Element container, ColumnAnalysisData data) {
        container.appendElement("h3").text("数据格式分析");
        
        Element box = container.appendElement("div").addClass("result-box negative");
        
        String formatDesc = "未检测到编码";
        if (data.formatType == DataFormatType.HEX) {
            formatDesc = "十六进制编码";
        } else if (data.formatType == DataFormatType.BASE64) {
            formatDesc = "Base64编码";
        }
        
        box.appendElement("p").html("<strong>检测格式:</strong> " + formatDesc);
        if (data.formatType != DataFormatType.RAW) {
            box.appendElement("p").html("<small>(数据将被还原为原始格式，用于后续的伪加密和弱加密分析)</small>");
        }
    }

    private void addPseudoEncryptionAnalysis(Element container, ColumnAnalysisData data) {
        container.appendElement("h3").text("伪加密分析");
        
        // 1. 数据分析
        container.appendElement("h4").text("1. 数据分析");
        
        // 数据长度分布
        Map<Integer, Integer> lengthDist = getDataLengthDistribution(data.decodedData);
        Element lengthBox = container.appendElement("div").addClass("chart");
        lengthBox.appendElement("p").html("<strong>数据长度分布表:</strong>");
        
        Element lengthTable = lengthBox.appendElement("table");
        Element thead = lengthTable.appendElement("thead");
        thead.appendElement("tr").html("<th>长度</th><th>出现次数</th><th>占比</th>");
        
        Element tbody = lengthTable.appendElement("tbody");
        int totalRecords = data.decodedData.size();
        for (Map.Entry<Integer, Integer> entry : lengthDist.entrySet()) {
            Element tr = tbody.appendElement("tr");
            tr.appendElement("td").text(String.valueOf(entry.getKey()));
            tr.appendElement("td").text(String.valueOf(entry.getValue()));
            tr.appendElement("td").text(String.format("%.2f%%", entry.getValue() * 100.0 / totalRecords));
        }
        
        // 字节分布热力图
        addHeatmapChart(container, data.decodedData);
        
        // 2. 伪加密分析
        container.appendElement("h4").text("2. 伪加密分析");
        
        // 获取伪加密分析结果
        int minLength = data.pseudoResult.getMinLength();
        int minLengthGap = data.pseudoResult.getMinLengthGap();
        double normalizedVariance = data.pseudoResult.getNormalizedVariance();
        
        Element analysisBox = container.appendElement("div").addClass("chart");
        
        // 分析最小长度
        analysisBox.appendElement("p").html("<strong>最小长度分析:</strong>");
        if (minLength < 16) {
            analysisBox.appendElement("p").text("最小长度为 " + minLength + "，长度偏低，为密文的可能性较低。");
        } else {
            analysisBox.appendElement("p").text("最小长度为 " + minLength + "，长度正常，为密文的可能性较高。");
        }
        
        // 分析长度间隔
        analysisBox.appendElement("p").html("<strong>最小长度间隔分析:</strong>");
        if (minLengthGap == Integer.MAX_VALUE) {
            // 所有长度都相同
            analysisBox.appendElement("p").text("所有长度都为 " + minLength + "，长度间隔正常，为密文的可能性较高。");
        } else if (minLengthGap < 16) {
            analysisBox.appendElement("p").text("最小长度间隔为 " + minLengthGap + "，长度间隔偏低，为密文的可能性较低。");
        } else {
            analysisBox.appendElement("p").text("最小长度间隔为 " + minLengthGap + "，长度间隔正常，为密文的可能性较高。");
        }
        
        // 分析归一化方差
        analysisBox.appendElement("p").html("<strong>归一化方差分析:</strong>");
        if (normalizedVariance >= 1.0) {
            analysisBox.appendElement("p").text("归一化方差为 " + String.format("%.4f", normalizedVariance) + 
                    "，归一化方差较大，为密文的可能性较低。");
        } else {
            analysisBox.appendElement("p").text("归一化方差为 " + String.format("%.4f", normalizedVariance) + 
                    "，归一化方差较小，为密文的可能性较高。");
        }
        
        // 3. 结论
        container.appendElement("h4").text("3. 结论");
        
        Element resultBox = container.appendElement("div")
                .addClass("result-box")
                .addClass(data.pseudoResult.isPseudoEncryption() ? "positive" : "negative");
        
        String conclusion = data.pseudoResult.isPseudoEncryption() ? 
                "⚠️ 检测到伪加密" : "✅ 未检测到伪加密";
        resultBox.appendElement("p").html("<strong>分析结论:</strong> " + conclusion);
        
        resultBox.appendElement("p").text("判断依据：" + data.pseudoResult.getReason());
    }

    private void addWeakEncryptionAnalysis(Element container, ColumnAnalysisData data) {
        container.appendElement("h3").text("弱加密分析");
        
        container.appendElement("h4").text("1. 数据分析");
        
        // 统计重复值（前20个）
        Map<String, Integer> repetitionMap = new HashMap<>();
        if (data.rawData != null) {
            for (String rawValue : data.rawData) {
                repetitionMap.put(rawValue, repetitionMap.getOrDefault(rawValue, 0) + 1);
            }
        }
        
        // 按重复次数降序排序
        List<Map.Entry<String, Integer>> sortedReps = new ArrayList<>(repetitionMap.entrySet());
        sortedReps.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        Element dataBox = container.appendElement("div").addClass("chart");
        int totalCount = data.rawData != null ? data.rawData.size() : 0;
        dataBox.appendElement("p").html("<strong>前20个重复值统计(抽取总数" + totalCount + "):</strong>");
        
        Element repTable = dataBox.appendElement("table");
        Element thead = repTable.appendElement("thead");
        thead.appendElement("tr").html("<th>排序</th><th>重复内容</th><th>重复次数</th><th>重复率</th>");
        
        Element tbody = repTable.appendElement("tbody");
        int count = 0;
        int maxRepetition = 0;
        
        for (Map.Entry<String, Integer> entry : sortedReps) {
            if (count >= 20) break;
            count++;
            maxRepetition = Math.max(maxRepetition, entry.getValue());
            
            Element tr = tbody.appendElement("tr");
            tr.appendElement("td").text(String.valueOf(count));
            String displayValue = entry.getKey().length() > 50 ? 
                    entry.getKey().substring(0, 50) + "..." : entry.getKey();
            tr.appendElement("td").text(displayValue);
            tr.appendElement("td").text(String.valueOf(entry.getValue()));
            if (totalCount > 0) {
                tr.appendElement("td").text(String.format("%.2f%%", entry.getValue() * 100.0 / totalCount));
            }
        }
        
        // 弱加密分析
        container.appendElement("h4").text("2. 弱加密分析");
        
        Element analysisBox = container.appendElement("div").addClass("chart");
        analysisBox.appendElement("p").text("真正加盐的加密数据重复率往往是很低的，出现相同数据可以断言密文数据没有加盐。" +
                "为了更加保险，我们将这个阈值设置为5，重复次数大于等于5次则认为数据没有加盐。");
        
        if (maxRepetition < 5) {
            analysisBox.appendElement("p").text("重复值统计最高值为 " + maxRepetition + 
                    "，重复率较低，为加盐密文的可能性较高。");
        } else {
            analysisBox.appendElement("p").text("重复值统计最高值为 " + maxRepetition + 
                    "，重复率较高，为加盐密文的可能性较低。");
        }
        
        // 弱加密结论
        container.appendElement("h4").text("3. 结论");

        Element resultBox = container.appendElement("div")
                .addClass("result-box")
                .addClass(data.weakResult.isWeakEncryption() ? "positive" : "negative");
        
        String conclusion = data.weakResult.isWeakEncryption() ? 
                "⚠️ 检测到弱加密" : "✅ 未检测到弱加密";
        resultBox.appendElement("p").html("<strong>分析结论:</strong> " + conclusion);
        resultBox.appendElement("p").text("判断依据：重复值统计最高值 " + maxRepetition + 
                (maxRepetition >= 5 ? " >= 5" : " < 5"));
    }

    private void addHeatmapChart(Element container, List<byte[]> decodedData) {
        // 计算字节分布
        int[] byteCount = new int[256];
        for (byte[] data : decodedData) {
            for (byte b : data) {
                byteCount[b & 0xFF]++;
            }
        }

        Element chartBox = container.appendElement("div").addClass("chart");
        
        chartBox.appendElement("p").html("<strong>字节分布热力图:</strong>");

        // 计算均值
        double sum = 0;
        for (int count : byteCount) {
            sum += count;
        }
        double mean = sum / 256;
        
        // 创建 16x16 表格
        Element table = chartBox.appendElement("table").addClass("heatmap-table");
        
        // 表头
        Element thead = table.appendElement("thead");
        Element headerRow = thead.appendElement("tr");
        headerRow.appendElement("th").text("");
        for (int i = 0; i < 16; i++) {
            headerRow.appendElement("th").text(String.format("%X", i));
        }
        
        // 数据行
        Element tbody = table.appendElement("tbody");
        for (int row = 0; row < 16; row++) {
            Element tr = tbody.appendElement("tr");
            tr.appendElement("th").text(String.format("%X", row) + "0");
            
            for (int col = 0; col < 16; col++) {
                int byteValue = row * 16 + col;
                int count = byteCount[byteValue];
                
                Element td = tr.appendElement("td");
                td.text(String.valueOf(count));
                
                // 根据值设置颜色
                String colorClass = "heat-0";
                if (count > 0) {
                    if (count <= mean / 3) {
                        colorClass = "heat-blue";
                    } else if (count <= mean * 2 / 3) {
                        colorClass = "heat-green";
                    } else if (count <= mean) {
                        colorClass = "heat-yellow";
                    } else if (count <= mean * 4 / 3) {
                        colorClass = "heat-orange";
                    } else {
                        colorClass = "heat-red";
                    }
                }
                td.addClass(colorClass);
            }
        }
        
        // 图例
        Element legend = chartBox.appendElement("div").attr("style", "margin-top: 10px;");
        String threshold1 = String.format("%.2f", mean / 3);
        String threshold2 = String.format("%.2f", mean * 2 / 3);
        String threshold3 = String.format("%.2f", mean);
        String threshold4 = String.format("%.2f", mean * 4 / 3);
        
        legend.appendElement("p").html("<small>图例: " +
                "<span style='background:#cccccc;padding:2px 5px;margin:2px;'>0</span>" +
                "<span style='background:#4285f4;color:white;padding:2px 5px;margin:2px;'>(0," + threshold1 + "]</span>" +
                "<span style='background:#34a853;color:white;padding:2px 5px;margin:2px;'>(" + threshold1 + "," + threshold2 + "]</span>" +
                "<span style='background:#fbbc04;padding:2px 5px;margin:2px;'>(" + threshold2 + "," + threshold3 + "]</span>" +
                "<span style='background:#fa8072;color:white;padding:2px 5px;margin:2px;'>(" + threshold3 + "," + threshold4 + "]</span>" +
                "<span style='background:#ea4335;color:white;padding:2px 5px;margin:2px;'>(" + threshold4 + ",+∞)</span>" +
                "</small>");
    }

    private void addFooter(Document doc) {
        Element container = doc.select(".container").first();
        Element footer = container.appendElement("div").addClass("footer");
        footer.appendElement("p").text("本报告由 SaltScan 自动生成");
        footer.appendElement("p").text("生成时间: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * 添加防篡改完整性校验
     * 计算容器文本内容的SHA-256哈希，嵌入页面并通过JavaScript在加载时校验
     */
    private void addIntegrityCheck(Document doc) {
        Element container = doc.select(".container").first();
        if (container == null) return;

        // 获取文本内容并归一化（与JavaScript端保持一致）
        String textContent = container.text()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        // 计算SHA-256哈希
        String hash = computeSHA256(textContent);

        // 在head中添加完整性哈希meta标签
        doc.head().appendElement("meta")
                .attr("name", "content-integrity")
                .attr("content", hash);

        // 在body末尾添加完整性校验脚本（位于container之外）
        Element script = doc.body().appendElement("script");
        script.appendChild(new DataNode(getIntegrityCheckScript()));
    }

    /**
     * 计算SHA-256哈希值
     */
    private String computeSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 获取前端完整性校验JavaScript脚本
     * 页面加载时自动校验容器文本内容的SHA-256哈希，若不匹配则显示红色背景和"内容不完整"提示
     */
    private String getIntegrityCheckScript() {
        return "(function(){" +
                "if(!window.crypto||!window.crypto.subtle)return;" +
                "var c=document.querySelector('.container');" +
                "if(!c)return;" +
                "var t=c.textContent.replace(/\\s+/g,' ').trim();" +
                "var d=new TextEncoder().encode(t);" +
                "crypto.subtle.digest('SHA-256',d).then(function(buf){" +
                "var a=Array.from(new Uint8Array(buf));" +
                "var h=a.map(function(b){return b.toString(16).padStart(2,'0');}).join('');" +
                "var m=document.querySelector('meta[name=\"content-integrity\"]');" +
                "if(!m)return;" +
                "var x=m.getAttribute('content');" +
                "if(h!==x){" +
                "document.body.style.backgroundColor='#ff0000';" +
                "var o=document.createElement('div');" +
                "o.style.cssText='text-align:center;padding:20px;margin:0 auto 20px auto;max-width:1400px;background:#cc0000;color:white;font-size:36px;font-weight:bold;border-radius:8px;';" +
                "o.textContent='\\u5185\\u5bb9\\u4e0d\\u5b8c\\u6574';" +
                "document.body.insertBefore(o,document.body.firstChild);" +
                "}" +
                "}).catch(function(e){console.error(e);});" +
                "})();";
    }

    // ================== 辅助方法 ==================

    /**
     * 计算香农熵
     */
    private double calculateEntropy(List<byte[]> decodedData) {
        // 统计字节频率
        Map<Integer, Integer> byteFreq = new HashMap<>();
        int totalBytes = 0;
        
        for (byte[] data : decodedData) {
            for (byte b : data) {
                int byteVal = b & 0xFF;
                byteFreq.put(byteVal, byteFreq.getOrDefault(byteVal, 0) + 1);
                totalBytes++;
            }
        }
        
        // 计算熵值
        double entropy = 0.0;
        for (int count : byteFreq.values()) {
            double probability = (double) count / totalBytes;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        
        return Math.min(entropy, 8.0); // 熵值最大为8
    }

    /**
     * 获取数据长度分布
     */
    private Map<Integer, Integer> getDataLengthDistribution(List<byte[]> decodedData) {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (byte[] data : decodedData) {
            int length = data.length;
            distribution.put(length, distribution.getOrDefault(length, 0) + 1);
        }
        // 按长度排序
        return new LinkedHashMap<>(distribution);
    }
}
