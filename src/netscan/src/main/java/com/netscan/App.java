package com.netscan;

import java.io.File;
import java.io.IOException;

/**
 * NetScan 主程序入口
 * 功能：依次调用网络扫描脚本，收集结果并生成HTML报告
 */
public class App {

    /** 脚本存放目录（相对于jar包所在目录） */
    private static final String SCRIPTS_DIR = "scripts";

    /** 报告输出目录（相对于jar包所在目录） */
    private static final String REPORT_DIR = "report";

    /** tcpdump类脚本的执行时长（秒） */
    private static final int TCPDUMP_DURATION_SECONDS = 60;

    public static void main(String[] args) {
        // 1. 打印软件启动横幅
        printBanner();

        // 获取jar包所在目录作为工作目录
        String baseDir = getBaseDir();
        String scriptsDir = baseDir + File.separator + SCRIPTS_DIR;
        String reportDir = "." + File.separator + REPORT_DIR;

        // 确保报告输出目录存在
        new File(reportDir).mkdirs();

        ScriptExecutor executor = new ScriptExecutor(scriptsDir);
        ReportGenerator reportGenerator = new ReportGenerator();

        String nmapResult = "";
        String dataAccessResult = "";
        String datanodeResult = "";

        try {
            // ========== 第一步：执行 nmap 扫描 ==========
            System.out.println("  ╔══════════════════════════════════════════════════╗");
            System.out.println("  ║  [1/3] 正在执行 nmap 网络节点发现...             ║");
            System.out.println("  ╚══════════════════════════════════════════════════╝");
            System.out.println();

            nmapResult = executor.executeWithRealtimeOutput("datanode_discovery_by_nmap.sh");
            // 按主机IP分组合并nmap结果
            nmapResult = ResultProcessor.mergeNmapByHost(nmapResult);
            System.out.println();
            System.out.println("  ✔ nmap 扫描完成！");
            System.out.println();

            // ========== 第二步：执行 tcpdump 数据访问发现 ==========
            System.out.println("  ╔══════════════════════════════════════════════════╗");
            System.out.println("  ║  [2/3] 正在执行 tcpdump 数据访问发现...          ║");
            System.out.println("  ║        预计执行时间：1 分钟（满1000条提前停止）  ║");
            System.out.println("  ╚══════════════════════════════════════════════════╝");
            System.out.println();

            // 使用 LineChecker：按 "--" 分隔符计数，达到1000条提前停止
            dataAccessResult = executor.executeWithProgress(
                    "data_access_discovery_by_tcpdump.sh",
                    TCPDUMP_DURATION_SECONDS,
                    new ScriptExecutor.LineChecker() {
                        @Override
                        public boolean shouldStop(String currentOutput) {
                            return ResultProcessor.countDataAccessRecords(currentOutput)
                                    >= ResultProcessor.MAX_RECORDS;
                        }
                    });
            // 截断结果确保不超过1000条
            dataAccessResult = ResultProcessor.truncateDataAccessResult(
                    dataAccessResult, ResultProcessor.MAX_RECORDS);
            int dataAccessCount = ResultProcessor.countDataAccessRecords(dataAccessResult);
            // 按连接对合并相同的记录（提取连接对、去ANSI码、分组合并）
            dataAccessResult = ResultProcessor.mergeDataAccessByConnection(dataAccessResult);
            System.out.println();
            System.out.println("  ✔ 数据访问发现完成！共收集 " + dataAccessCount + " 条记录");
            System.out.println();

            // ========== 第三步：执行 tcpdump 数据节点发现 ==========
            System.out.println("  ╔══════════════════════════════════════════════════╗");
            System.out.println("  ║  [3/3] 正在执行 tcpdump 数据节点发现...          ║");
            System.out.println("  ║        预计执行时间：1 分钟（去重满1000条停止）  ║");
            System.out.println("  ╚══════════════════════════════════════════════════╝");
            System.out.println();

            // 使用 LineChecker：忽略源端口去重后达到1000条提前停止
            datanodeResult = executor.executeWithProgress(
                    "datanode_discovery_by_tcpdump.sh",
                    TCPDUMP_DURATION_SECONDS,
                    new ScriptExecutor.LineChecker() {
                        @Override
                        public boolean shouldStop(String currentOutput) {
                            return ResultProcessor.countDeduplicatedDatanodeRecords(currentOutput)
                                    >= ResultProcessor.MAX_RECORDS;
                        }
                    });
            // 对结果做提取IP（去掉两端端口） + 去重 + 截断处理
            datanodeResult = ResultProcessor.deduplicateDatanodeResult(
                    datanodeResult, ResultProcessor.MAX_RECORDS);
            int datanodeCount = datanodeResult.isEmpty() ? 0 : datanodeResult.split("\n").length;
            System.out.println();
            System.out.println("  ✔ 数据节点发现完成！共收集 " + datanodeCount + " 条去重记录");
            System.out.println();

        } catch (IOException | InterruptedException e) {
            System.err.println("  ✘ 脚本执行出错: " + e.getMessage());
            e.printStackTrace();
        }

        // ========== 生成HTML报告 ==========
        String reportPath = reportDir + File.separator + "netscan-report.html";
        try {
            reportGenerator.generateReport(reportPath, nmapResult, dataAccessResult, datanodeResult);
        } catch (IOException e) {
            System.err.println("  ✘ 报告生成失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // ========== 输出完成信息 ==========
        printFinish(reportPath);
    }

    /**
     * 打印软件启动横幅，美观展示软件信息
     */
    static void printBanner() {
        System.out.println();
        System.out.println("  ███╗   ██╗███████╗████████╗███████╗ ██████╗ █████╗ ███╗   ██╗");
        System.out.println("  ████╗  ██║██╔════╝╚══██╔══╝██╔════╝██╔════╝██╔══██╗████╗  ██║");
        System.out.println("  ██╔██╗ ██║█████╗     ██║   ███████╗██║     ███████║██╔██╗ ██║");
        System.out.println("  ██║╚██╗██║██╔══╝     ██║   ╚════██║██║     ██╔══██║██║╚██╗██║");
        System.out.println("  ██║ ╚████║███████╗   ██║   ███████║╚██████╗██║  ██║██║ ╚████║");
        System.out.println("  ╚═╝  ╚═══╝╚══════╝   ╚═╝   ╚══════╝ ╚═════╝╚═╝  ╚═╝╚═╝  ╚═══╝");
        System.out.println();
        System.out.println("  ╭──────────────────────────────────────────────────╮");
        System.out.println("  │         NetScan - 网络主机节点发现工具           │");
        System.out.println("  │                                                  │");
        System.out.println("  │  功能：扫描网络中的主机节点和数据访问行为        │");
        System.out.println("  │  版本：1.0.0                                     │");
        System.out.println("  ╰──────────────────────────────────────────────────╯");
        System.out.println();
    }

    /**
     * 打印执行完成信息，告知用户报告地址
     *
     * @param reportPath 报告文件的绝对路径
     */
    static void printFinish(String reportPath) {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║            ✔  所有扫描任务已完成！               ║");
        System.out.println("  ╚══════════════════════════════════════════════════╝");
        System.out.println("  报告地址：");
        System.out.println("  " + reportPath);
        System.out.println();
    }

    /**
     * 获取程序的工作基础目录
     * 优先使用jar包所在目录，否则使用当前工作目录
     *
     * @return 基础目录路径
     */
    static String getBaseDir() {
        // 尝试获取jar包所在目录
        try {
            String jarPath = App.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                // 运行的是jar包，返回jar包所在目录
                return jarFile.getParent();
            }
        } catch (Exception e) {
            // 忽略异常，使用当前目录
        }
        // 开发环境或其他情况，返回当前工作目录
        return System.getProperty("user.dir");
    }
}
