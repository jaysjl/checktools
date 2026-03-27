package com.dbscan;

import com.dbscan.config.Config;
import com.dbscan.config.ConfigLoader;
import com.dbscan.db.DatabaseConnector;
import com.dbscan.scan.DataScanner;
import com.dbscan.scan.ReportGenerator;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 数据库敏感数据扫描工具主程序
 */
public class Main {
    public static void main(String[] args) {
        // 检查命令行参数
        if (args.length == 0 || !args[0].startsWith("--config")) {
            printUsage();
            System.exit(1);
        }

        String configPath = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--config") && i + 1 < args.length) {
                configPath = args[i + 1];
                break;
            }
        }

        if (configPath == null || configPath.isEmpty()) {
            System.err.println("错误: 配置文件路径不能为空");
            printUsage();
            System.exit(1);
        }

        try {
            // 加载配置
            System.out.println("加载配置文件: " + configPath);
            Config config = ConfigLoader.loadConfig(configPath);
            ConfigLoader.validateConfig(config);
            System.out.println("配置加载成功");

            // 创建数据库连接器
            DatabaseConnector connector = new DatabaseConnector(config.getJdbc());

            // 测试连接
            System.out.println("测试数据库连接...");
            connector.connect();
            System.out.println("数据库连接成功");

            // 执行扫描
            DataScanner scanner = new DataScanner(connector, config);
            scanner.scan();

            // 生成报告
            ReportGenerator.generateReport(scanner.getResults(), config.getOutput().getPath());
            ReportGenerator.printSummary(scanner.getResults());

            // 关闭连接
            connector.close();
            System.out.println("扫描工具执行完成");

        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("配置验证失败: " + e.getMessage());
            System.exit(1);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("运行出错: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("使用方法:");
        System.out.println("  java -jar dbscan.jar --config <配置文件路径>");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar dbscan.jar --config config.json");
    }
}
