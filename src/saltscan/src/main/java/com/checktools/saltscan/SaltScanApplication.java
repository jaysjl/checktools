package com.checktools.saltscan;

import com.checktools.saltscan.analysis.DataFormatAnalyzer;
import com.checktools.saltscan.analysis.DataFormatType;
import com.checktools.saltscan.analysis.PseudoEncryptionAnalyzer;
import com.checktools.saltscan.analysis.WeakEncryptionAnalyzer;
import com.checktools.saltscan.config.ConfigManager;
import com.checktools.saltscan.db.DatabaseConnector;
import com.checktools.saltscan.report.ReportGenerator;
import com.checktools.saltscan.testdata.TestDataGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SaltScan 主应用程序
 */
public class SaltScanApplication {

    public static void main(String[] args) {

        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        try {
            String configPath = null;
            boolean importMode = false;
            String reportPath = null;

            // 解析命令行参数
            for (int i = 0; i < args.length; i++) {
                if ("--config".equals(args[i]) && i + 1 < args.length) {
                    configPath = args[i + 1];
                    i++;
                } else if ("--import".equals(args[i])) {
                    importMode = true;
                } else if ("--report".equals(args[i]) && i + 1 < args.length) {
                    reportPath = args[i + 1];
                    i++;
                } else if (!args[i].startsWith("--")) {
                    configPath = args[i];
                }
            }

            if (configPath == null) {
                System.err.println("错误：必须指定配置文件路径");
                printUsage();
                System.exit(1);
            }

            System.out.println("[INFO] 开始执行扫描任务，配置文件: " + configPath);

            // 1. 加载配置
            ConfigManager configManager = new ConfigManager(configPath);
            System.out.println("[INFO] 配置加载成功");

            // 2. 连接数据库
            DatabaseConnector connector = new DatabaseConnector(configManager);
            connector.connect();

            if (importMode) {
                // 导入模式：生成测试数据
                System.out.println("[INFO] 进入数据导入模式");
                TestDataGenerator generator = new TestDataGenerator(connector, configManager);
                generator.importTestData();
                System.out.println("[INFO] 数据导入完成");
                connector.close();
                System.exit(0);
            }

            // 3. 获取扫描目标列表
            List<Map<String, Object>> targets = configManager.getScanTargets();
            if (targets == null || targets.isEmpty()) {
                System.err.println("[ERROR] 未配置扫描目标");
                System.exit(1);
            }

            // 4. 生成报告
            String outputPath = configManager.getOutputPath();
            ReportGenerator reportGenerator = new ReportGenerator(connector, configManager);

            // 处理每个扫描目标
            for (Map<String, Object> target : targets) {
                String tableName = (String) target.get("table");
                List<String> columns = (List<String>) target.get("columns");

                if (tableName == null || columns == null || columns.isEmpty()) {
                    continue;
                }

                System.out.println("[INFO] 处理表: " + tableName);

                // 验证权限
                if (!connector.validatePermission(tableName)) {
                    System.err.println("[ERROR] 无权限访问表: " + tableName);
                    continue;
                }

                // 处理每个列
                for (String columnName : columns) {
                    System.out.println("[INFO] 扫描列: " + tableName + "." + columnName);

                    try {
                        // 查询数据
                        int limit = configManager.getScanLimit();
                        com.checktools.saltscan.db.QueryResult queryResult = connector.queryColumnDataWithMetadata(tableName, columnName, limit);
                        List<String> rawData = queryResult.getData();
                        
                        if (rawData == null || rawData.isEmpty()) {
                            System.out.println("[WARN] 未查询到数据: " + tableName + "." + columnName);
                            continue;
                        }

                        System.out.println("[INFO] 从表 " + tableName + " 的列 " + columnName + " 查询到 " + rawData.size() + " 条数据");

                        // 分析数据格式
                        DataFormatAnalyzer formatAnalyzer = new DataFormatAnalyzer();
                        // 如果数据来自BLOB列，则识别为RAW格式，不进行自动检测
                        DataFormatType formatType = formatAnalyzer.analyzeFormat(rawData.get(0), queryResult.isFromBlobColumn());
                        System.out.println("[INFO] 检测到数据格式: " + formatType.getValue());

                        // 还原数据格式
                        List<byte[]> decodedData = new ArrayList<>();
                        for (String data : rawData) {
                            byte[] decoded;
                            // 如果数据来自BLOB列（已被转换为HEX），需要进行HEX解码
                            if (queryResult.isFromBlobColumn() && formatType == DataFormatType.RAW) {
                                decoded = formatAnalyzer.decodeHexFromBlob(data);
                            } else {
                                decoded = formatAnalyzer.decode(data, formatType);
                            }
                            decodedData.add(decoded);
                        }

                        // 伪加密分析
                        int dataLengthMin = Integer.parseInt(configManager.getProperty("scan.data_length_min"));
                        int dataLengthIntervalMin = Integer.parseInt(configManager.getProperty("scan.data_length_interval_min"));
                        double dataVarianceMax = Double.parseDouble(configManager.getProperty("scan.data_variance_max"));
                        PseudoEncryptionAnalyzer pseudoAnalyzer = new PseudoEncryptionAnalyzer(dataLengthMin, dataLengthIntervalMin, dataVarianceMax);
                        PseudoEncryptionAnalyzer.PseudoEncryptionResult pseudoResult = pseudoAnalyzer.analyze(decodedData);
                        pseudoResult.setRawData(rawData); // 保存原始数据用于弱加密分析显示
                        System.out.println("[INFO] 伪加密分析完成: " + (pseudoResult.isPseudoEncryption() ? "检测到伪加密" : "未检测到伪加密"));

                        // 弱加密分析
                        int repetitionCountMax = Integer.parseInt(configManager.getProperty("scan.data_repetition_count_max"));
                        WeakEncryptionAnalyzer weakAnalyzer = new WeakEncryptionAnalyzer(repetitionCountMax);
                        WeakEncryptionAnalyzer.WeakEncryptionResult weakResult = weakAnalyzer.analyze(rawData);
                        int maxRepetitionCount = weakResult.getMaxRepetitionCount();
                        System.out.println("[INFO] 弱加密分析完成: 最高重复值 " + maxRepetitionCount);

                        // 添加分析结果到报告
                        reportGenerator.addColumnAnalysis(tableName, columnName, formatType, pseudoResult, weakResult, decodedData, rawData);

                    } catch (Exception e) {
                        System.err.println("[ERROR] 扫描列失败: " + tableName + "." + columnName);
                        e.printStackTrace();
                    }
                }
            }

            // 生成最终报告
            reportGenerator.generateReport(outputPath);
            System.out.println("[INFO] 报告生成成功: " + outputPath);

            // 关闭连接
            connector.close();

            System.out.println("[INFO] 扫描任务完成");

        } catch (Exception e) {
            System.err.println("[ERROR] 执行过程中出错");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("使用方法: java -jar saltscan-<version>.jar [--config] <config.json> [OPTIONS]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --config <path>    配置文件路径（必须）");
        System.out.println("  --import           导入测试数据到数据库");
        System.out.println("  --report <path>    报告文件路径（用于验证）");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  # 导入测试数据");
        System.out.println("  java -jar saltscan-1.0.0.jar --config config-example.json --import");
        System.out.println();
        System.out.println("  # 执行扫描");
        System.out.println("  java -jar saltscan-1.0.0.jar --config config-example.json");
        System.out.println();
        System.out.println("  # 验证报告");
        System.out.println("  java -jar saltscan-1.0.0.jar --config config-example.json --report ./report.html");
    }
}
