package com.dbscan.scan;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * ReportGenerator 单元测试
 */
public class ReportGeneratorTest {

    private List<ScanResult> results;
    private String testReportPath = "./test_report.html";

    @Before
    public void setUp() {
        results = Arrays.asList(
                new ScanResult("192.168.1.1", 3306, "MySQL", "db1", "public", "users", "phone", 100L, "phone", "手机号", 50.0, "13800138000; 13800138001"),
                new ScanResult("192.168.1.1", 3306, "MySQL", "db1", "public", "users", "id_card", 100L, "id", "身份证号", 100.0, "110101199001011234"),
                new ScanResult("192.168.1.1", 3306, "MySQL", "db1", "public", "orders", "phone", 200L, "phone", "手机号", 75.0, "13912345678")
        );
    }

    @Test
    public void testGenerateReport() throws IOException {
        ReportGenerator.generateReport(results, testReportPath);

        File file = new File(testReportPath);
        assertTrue("报告文件应该被创建", file.exists());

        String content = new String(Files.readAllBytes(Paths.get(testReportPath)), java.nio.charset.StandardCharsets.UTF_8);

        // 验证 HTML 结构
        assertTrue("报告应该是 HTML 文档", content.contains("<!DOCTYPE html>"));
        assertTrue("报告应该包含表头", content.contains("<th>IP</th>"));

        // 验证数据行
        assertTrue("报告应该包含第一条记录的 IP", content.contains("192.168.1.1"));
        assertTrue("报告应该包含第一条记录的数据库类型", content.contains("MySQL"));
        assertTrue("报告应该包含匹配率百分比", content.contains("50.00%"));

        // 验证篡改提示
        assertTrue("报告应该包含篡改提示文案", content.contains("内容不完整"));
        assertFalse("未篡改时不应显示校验通过文案", content.contains("报告完整性校验通过"));

        // 清理
        file.delete();
    }

    @Test
    public void testHtmlEscapingAndChecksum() throws IOException {
        List<ScanResult> testResults = Arrays.asList(
                new ScanResult("192.168.1.1", 3306, "MySQL", "db1", "public", "test", "col1", 100L, "rule", "description", 50.0, "value,with,comma"),
                new ScanResult("192.168.1.1", 3306, "MySQL", "db1", "public", "test", "col2", 100L, "rule", "description", 75.0, "value\"with\"quote"),
                new ScanResult("192.168.1.1", 3306, "MySQL", "db1", "public", "test", "col3", 100L, "rule", "description", 100.0, "<script>alert('x')</script>")
        );

        ReportGenerator.generateReport(testResults, testReportPath);

        String content = new String(Files.readAllBytes(Paths.get(testReportPath)), java.nio.charset.StandardCharsets.UTF_8);

        // 验证 HTML 转义
        assertTrue("HTML 中应保留普通文本", content.contains("value,with,comma"));
        assertTrue("引号应被 HTML 转义", content.contains("value&quot;with&quot;quote"));
        assertTrue("脚本标签应被转义", content.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));

        // 验证校验和稳定性
        String payload = ReportGenerator.buildChecksumPayload(testResults, "2026-01-01 00:00:00");
        String checksum = ReportGenerator.sha256(payload);
        assertEquals("校验和长度应为 64 位十六进制", 64, checksum.length());

        // 同一输入产生同一校验和
        String checksum2 = ReportGenerator.sha256(payload);
        assertEquals("相同载荷应产生相同校验和", checksum, checksum2);

        // 清理
        new File(testReportPath).delete();
    }

    @Test
    public void testEmptyResults() throws IOException {
        ReportGenerator.generateReport(new java.util.ArrayList<>(), testReportPath);

        File file = new File(testReportPath);
        assertTrue("空结果也应该生成报告文件", file.exists());

        String content = new String(Files.readAllBytes(Paths.get(testReportPath)), java.nio.charset.StandardCharsets.UTF_8);

        // 空报告
        assertTrue("空报告应该包含 HTML 表格", content.contains("<table>"));
        assertTrue("空报告应该提示未发现命中", content.contains("未发现敏感数据命中"));

        // 清理
        file.delete();
    }
}
