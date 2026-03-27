package com.checktools.filescan;

import com.checktools.filescan.model.AnalysisResult;
import com.checktools.filescan.model.SensitiveData;
import com.checktools.filescan.model.SensitiveData.DataType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * ReportGenerator 单元测试
 * 测试HTML报告生成器的各项功能，包括报告内容、防篡改校验等
 */
public class ReportGeneratorTest {

    private ReportGenerator generator;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        generator = new ReportGenerator();
    }

    /**
     * 测试：生成基本的HTML报告
     */
    @Test
    public void testGenerateBasicReport() throws IOException {
        AnalysisResult result = createSampleResult();

        File outputFile = new File(tempFolder.getRoot(), "test_report.html");
        generator.generateReport(result, outputFile.getAbsolutePath());

        assertTrue("报告文件应存在", outputFile.exists());
        assertTrue("报告文件不应为空", outputFile.length() > 0);

        String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("应包含HTML标签", content.contains("<!DOCTYPE html>"));
        assertTrue("应包含报告标题", content.contains("文件扫描分析报告"));
    }

    /**
     * 测试：报告中应包含文件基本信息
     */
    @Test
    public void testReportContainsFileInfo() {
        AnalysisResult result = createSampleResult();

        String html = generator.buildHtmlContent(result);

        assertTrue("应包含文件路径", html.contains("/test/sample.dat"));
        assertTrue("应包含可见字符数", html.contains("15"));
    }

    /**
     * 测试：报告中应包含敏感数据信息
     */
    @Test
    public void testReportContainsSensitiveData() {
        AnalysisResult result = createSampleResult();
        result.addSensitiveData(new SensitiveData(DataType.PHONE, "13912345678", 5));
        result.addSensitiveData(new SensitiveData(DataType.EMAIL, "test@example.com", 20));

        String html = generator.buildHtmlContent(result);

        assertTrue("应包含手机号类型", html.contains("手机号"));
        assertTrue("应包含邮箱类型", html.contains("邮箱"));
    }

    /**
     * 测试：报告中应包含可见字符内容
     */
    @Test
    public void testReportContainsVisibleChars() {
        AnalysisResult result = createSampleResult();

        String html = generator.buildHtmlContent(result);

        assertTrue("应包含可见字符内容", html.contains("HelloWorld12345"));
    }

    /**
     * 测试：报告应包含防篡改哈希值
     */
    @Test
    public void testReportContainsHash() {
        AnalysisResult result = createSampleResult();

        String html = generator.buildHtmlContent(result);

        assertTrue("应包含哈希值容器", html.contains("content-hash"));
        assertTrue("应包含防篡改脚本", html.contains("crypto.subtle.digest"));
        assertTrue("应包含篡改警告", html.contains("内容不完整"));
        assertTrue("应包含tamper-warning", html.contains("tamper-warning"));
    }

    /**
     * 测试：防篡改哈希值应可重复计算
     */
    @Test
    public void testHashConsistency() {
        String content = "测试内容一致性";

        String hash1 = ReportGenerator.calculateHash(content);
        String hash2 = ReportGenerator.calculateHash(content);

        assertEquals("相同内容的哈希值应一致", hash1, hash2);
        assertEquals("SHA-256哈希应为64个十六进制字符", 64, hash1.length());
    }

    /**
     * 测试：不同内容的哈希值应不同
     */
    @Test
    public void testHashDifference() {
        String hash1 = ReportGenerator.calculateHash("内容A");
        String hash2 = ReportGenerator.calculateHash("内容B");

        assertNotEquals("不同内容的哈希值应不同", hash1, hash2);
    }

    /**
     * 测试：HTML特殊字符转义
     */
    @Test
    public void testEscapeHtml() {
        assertEquals("&amp;", ReportGenerator.escapeHtml("&"));
        assertEquals("&lt;", ReportGenerator.escapeHtml("<"));
        assertEquals("&gt;", ReportGenerator.escapeHtml(">"));
        assertEquals("&quot;", ReportGenerator.escapeHtml("\""));
        assertEquals("&#39;", ReportGenerator.escapeHtml("'"));
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
                ReportGenerator.escapeHtml("<script>alert(1)</script>"));
        assertEquals("", ReportGenerator.escapeHtml(null));
    }

    /**
     * 测试：敏感数据脱敏处理
     */
    @Test
    public void testMaskSensitiveContent() {
        // 手机号脱敏
        SensitiveData phone = new SensitiveData(DataType.PHONE, "13912345678", 0);
        String maskedPhone = ReportGenerator.maskSensitiveContent(phone);
        assertEquals("139****5678", maskedPhone);

        // 邮箱脱敏
        SensitiveData email = new SensitiveData(DataType.EMAIL, "testuser@example.com", 0);
        String maskedEmail = ReportGenerator.maskSensitiveContent(email);
        assertEquals("te***@example.com", maskedEmail);

        // 身份证号脱敏
        SensitiveData idCard = new SensitiveData(DataType.ID_CARD, "110101199003070011", 0);
        String maskedId = ReportGenerator.maskSensitiveContent(idCard);
        assertEquals("110101********0011", maskedId);
    }

    /**
     * 测试：文件大小格式化
     */
    @Test
    public void testFormatFileSize() {
        assertEquals("0 B", ReportGenerator.formatFileSize(0));
        assertEquals("512 B", ReportGenerator.formatFileSize(512));
        assertEquals("1.00 KB", ReportGenerator.formatFileSize(1024));
        assertEquals("1.50 KB", ReportGenerator.formatFileSize(1536));
        assertEquals("1.00 MB", ReportGenerator.formatFileSize(1024 * 1024));
        assertEquals("1.00 GB", ReportGenerator.formatFileSize(1024L * 1024 * 1024));
    }

    /**
     * 测试：无敏感数据时的报告
     */
    @Test
    public void testReportWithNoSensitiveData() {
        AnalysisResult result = new AnalysisResult("/test/clean.dat");
        result.setFileSize(100);
        result.setVisibleChars("cleandata");
        result.setVisibleCharCount(9);

        String html = generator.buildHtmlContent(result);

        assertTrue("应包含'未检测到敏感数据'", html.contains("未检测到敏感数据"));
    }

    /**
     * 测试：达到1000000字符上限时的报告
     */
    @Test
    public void testReportWithReachedLimit() {
        AnalysisResult result = new AnalysisResult("/test/large.dat");
        result.setFileSize(5000000);
        result.setVisibleChars("XXXXX");
        result.setVisibleCharCount(1000000);
        result.setReachedLimit(true);

        String html = generator.buildHtmlContent(result);

        assertTrue("应提示已达上限", html.contains("已达上限1000000"));
    }

    /**
     * 测试：报告写入文件功能
     */
    @Test
    public void testGenerateReportToFile() throws IOException {
        AnalysisResult result = createSampleResult();
        result.addSensitiveData(new SensitiveData(DataType.PHONE, "13912345678", 0));

        File outputFile = new File(tempFolder.getRoot(), "output_report.html");
        generator.generateReport(result, outputFile.getAbsolutePath());

        assertTrue("输出文件应存在", outputFile.exists());
        String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertTrue("文件内容应为完整HTML", content.startsWith("<!DOCTYPE html>"));
        assertTrue("文件应包含防篡改脚本", content.contains("SHA-256"));
    }

    /**
     * 测试：stripHtmlForHash方法能正确提取纯文本并归一化
     */
    @Test
    public void testStripHtmlForHash() {
        String html = "        <h1>标题</h1>\n        <td>test &amp; data</td>\n        <p>hello</p>";
        String text = ReportGenerator.stripHtmlForHash(html);

        assertEquals("标题 test & data hello", text);
        assertFalse("不应包含HTML标签", text.contains("<"));
        assertFalse("不应包含HTML实体", text.contains("&amp;"));
    }

    /**
     * 测试：防篡改哈希在buildHtmlContent中嵌入的值应与bodyContent的文本哈希一致
     */
    @Test
    public void testAntiTamperHashConsistency() {
        AnalysisResult result = createSampleResult();

        // 构建body内容
        String bodyContent = generator.buildBodyContent(result);
        // Java端：strip后计算哈希
        String textForHash = ReportGenerator.stripHtmlForHash(bodyContent);
        String expectedHash = ReportGenerator.calculateHash(textForHash);

        // 完整HTML中应包含相同的哈希
        String html = generator.buildHtmlContent(result);
        assertTrue("HTML应包含计算出的哈希值", html.contains(expectedHash));
    }

    /**
     * 创建测试用的样本分析结果
     */
    private AnalysisResult createSampleResult() {
        AnalysisResult result = new AnalysisResult("/test/sample.dat");
        result.setFileSize(1024);
        result.setVisibleChars("HelloWorld12345");
        result.setVisibleCharCount(15);
        result.setReachedLimit(false);
        return result;
    }
}
