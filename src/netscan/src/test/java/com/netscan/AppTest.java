package com.netscan;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

/**
 * App主类测试
 */
public class AppTest {

    /**
     * 测试printBanner方法输出不为空且包含关键信息
     */
    @Test
    public void testPrintBanner() {
        // 捕获System.out输出
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            App.printBanner();
            String output = baos.toString();

            // Banner应包含软件名
            assertTrue("Banner应包含NetScan", output.contains("NetScan"));
            // Banner应包含版本号
            assertTrue("Banner应包含版本号", output.contains("1.0.0"));
            // Banner应包含功能描述
            assertTrue("Banner应包含功能描述", output.contains("网络主机节点发现工具"));
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * 测试printFinish方法输出包含报告路径
     */
    @Test
    public void testPrintFinish() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            String testPath = "/tmp/test_report.html";
            App.printFinish(testPath);
            String output = baos.toString();

            // 完成信息应包含报告路径
            assertTrue("应包含报告路径", output.contains(testPath));
            // 应包含完成提示
            assertTrue("应包含完成提示", output.contains("完成"));
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * 测试getBaseDir方法返回有效路径
     */
    @Test
    public void testGetBaseDir() {
        String baseDir = App.getBaseDir();
        assertNotNull("基础目录不应为null", baseDir);
        assertTrue("基础目录不应为空", baseDir.length() > 0);
    }
}
