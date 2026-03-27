package com.checktools.filescan;

import com.checktools.filescan.model.AnalysisResult;

import java.io.File;
import java.io.IOException;

/**
 * 文件扫描分析工具 - 主程序入口
 * 用来分析加密后的文件是否真的加密
 *
 * 功能：
 * 1. 读取二进制文件中的数据，分析存在的可见字符（中文、英文、数字）
 * 2. 分析前2000个可见字符，达到2000就停止
 * 3. 分析可见字符中的"身份证号"、"手机号"、"邮箱"等敏感数据
 * 4. 生成包含敏感数据和可见字符的HTML报告
 * 5. 报告带有防篡改功能
 *
 * 使用方法：
 *   java -jar filescan.jar <待分析文件路径> [报告输出路径]
 */
public class FileScanApp {

    public static void main(String[] args) {
        // 参数校验
        if (args.length < 1) {
            System.out.println("用法: java -jar filescan.jar <待分析文件路径> [报告输出路径]");
            System.out.println("参数说明:");
            System.out.println("  <待分析文件路径>  必填，要分析的文件路径");
            System.out.println("  [报告输出路径]    可选，HTML报告输出路径，默认为 report/filescan-report.html");
            System.exit(1);
        }

        String inputFilePath = args[0];
        String outputPath = args.length >= 2 ? args[1] : "./report/filescan-report.html";

        System.out.println("====================================");
        System.out.println("  文件扫描分析工具 v1.0");
        System.out.println("====================================");
        System.out.println("分析文件: " + inputFilePath);
        System.out.println("报告输出: " + outputPath);
        System.out.println();

        try {
            // 第一步：分析文件
            System.out.println("[1/2] 正在分析文件...");
            FileAnalyzer analyzer = new FileAnalyzer();
            AnalysisResult result = analyzer.analyze(inputFilePath);

            System.out.println("  文件大小: " + result.getFileSize() + " 字节");
            System.out.println("  可见字符数: " + result.getVisibleCharCount()
                    + (result.isReachedLimit() ? " (已达上限)" : ""));
            System.out.println("  敏感数据: " + result.getSensitiveDataList().size() + " 条");

            // 第二步：生成报告
            System.out.println("[2/2] 正在生成HTML报告...");
            ReportGenerator generator = new ReportGenerator();
            generator.generateReport(result, outputPath);

            System.out.println();
            System.out.println("分析完成！报告已保存至: " + outputPath);

        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
            System.exit(2);
        }
    }
    
}
