package com.checktools.filescan;

import com.checktools.filescan.model.AnalysisResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * FileAnalyzer 单元测试
 * 测试文件分析器的各项功能（基于 personal_data_discovery_from_file.sh 脚本提取可见字符）
 */
public class FileAnalyzerTest {

    private FileAnalyzer analyzer;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        // Maven 从项目根目录运行测试，脚本在 scripts/ 下
        String scriptPath = new File("scripts/personal_data_discovery_from_file.sh").getAbsolutePath();
        analyzer = new FileAnalyzer(scriptPath);
    }

    /**
     * 测试：分析包含纯ASCII英文文本的文件
     */
    @Test
    public void testAnalyzeAsciiTextFile() throws IOException {
        File testFile = tempFolder.newFile("ascii_test.txt");
        writeTextFile(testFile, "Hello World 12345");

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertNotNull(result);
        assertEquals(testFile.getAbsolutePath(), result.getFilePath());
        assertTrue(result.getVisibleCharCount() > 0);
        assertNotNull(result.getVisibleChars());
        assertTrue(result.getVisibleChars().contains("Hello"));
        assertTrue(result.getVisibleChars().contains("12345"));
        assertFalse(result.isReachedLimit());
    }

    /**
     * 测试：分析包含中文文本的文件
     */
    @Test
    public void testAnalyzeChineseTextFile() throws IOException {
        File testFile = tempFolder.newFile("chinese_test.txt");
        writeTextFile(testFile, "你好世界测试文件");

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertNotNull(result);
        assertTrue(result.getVisibleCharCount() > 0);
        assertTrue(result.getVisibleChars().contains("你好"));
        assertTrue(result.getVisibleChars().contains("世界"));
    }

    /**
     * 测试：分析包含混合内容（中英文、数字）的文件
     */
    @Test
    public void testAnalyzeMixedContentFile() throws IOException {
        File testFile = tempFolder.newFile("mixed_test.txt");
        writeTextFile(testFile, "用户张三的手机号是13800138000，邮箱是test@example.com");

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertNotNull(result);
        assertTrue(result.getVisibleCharCount() > 0);
        String visibleChars = result.getVisibleChars();
        assertTrue(visibleChars.contains("用户"));
        assertTrue(visibleChars.contains("13800138000"));
        assertTrue(visibleChars.contains("test@example.com"));
    }

    /**
     * 测试：分析包含敏感数据的文件，应能检测到手机号
     */
    @Test
    public void testAnalyzeFileWithSensitiveData() throws IOException {
        File testFile = tempFolder.newFile("sensitive_test.txt");
        writeTextFile(testFile, "联系电话13912345678请拨打");

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertTrue(result.hasSensitiveData());
        assertTrue(result.getSensitiveDataList().size() >= 1);
    }

    /**
     * 测试：分析二进制文件（包含不可见字符混合可见文本）
     */
    @Test
    public void testAnalyzeBinaryFile() throws IOException {
        File testFile = tempFolder.newFile("binary_test.bin");
        // 写入混合的二进制数据和可见文本
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            // 写入一些不可见字节
            fos.write(new byte[]{0x00, 0x01, 0x02, 0x03});
            // 写入可见ASCII文本（需要超过3个字符才能被脚本保留）
            fos.write("ABCDEF123456".getBytes(StandardCharsets.US_ASCII));
            // 写入更多不可见字节
            fos.write(new byte[]{0x00, 0x00, 0x10, 0x11});
            // 写入更多可见文本（超过3字符）
            fos.write("HelloWorld".getBytes(StandardCharsets.US_ASCII));
        }

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertNotNull(result);
        String visibleChars = result.getVisibleChars();
        assertTrue("应包含ABCDEF123456", visibleChars.contains("ABCDEF123456"));
        assertTrue("应包含HelloWorld", visibleChars.contains("HelloWorld"));
    }

    /**
     * 测试：可见字符达到10000上限时停止
     */
    @Test
    public void testVisibleCharLimit() throws IOException {
        File testFile = tempFolder.newFile("large_test.txt");
        // 创建超过1000000个可见字符的文件
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1200000; i++) {
            sb.append('A');
        }
        writeTextFile(testFile, sb.toString());

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertEquals(1000000, result.getVisibleCharCount());
        assertTrue(result.isReachedLimit());
        assertEquals(1000000, result.getVisibleChars().length());
    }

    /**
     * 测试：文件不存在时抛出异常
     */
    @Test(expected = IOException.class)
    public void testAnalyzeNonExistentFile() throws IOException {
        analyzer.analyze("/non/existent/file.txt");
    }

    /**
     * 测试：空文件分析
     */
    @Test
    public void testAnalyzeEmptyFile() throws IOException {
        File testFile = tempFolder.newFile("empty_test.txt");

        AnalysisResult result = analyzer.analyze(testFile.getAbsolutePath());

        assertNotNull(result);
        assertEquals(0, result.getVisibleCharCount());
        assertEquals("", result.getVisibleChars());
        assertFalse(result.isReachedLimit());
        assertFalse(result.hasSensitiveData());
    }

    /**
     * 测试：isVisibleAscii方法
     */
    @Test
    public void testIsVisibleAscii() {
        assertTrue(FileAnalyzer.isVisibleAscii('A'));
        assertTrue(FileAnalyzer.isVisibleAscii('z'));
        assertTrue(FileAnalyzer.isVisibleAscii('0'));
        assertTrue(FileAnalyzer.isVisibleAscii('9'));
        assertTrue(FileAnalyzer.isVisibleAscii('@'));
        assertTrue(FileAnalyzer.isVisibleAscii('!'));
        assertTrue(FileAnalyzer.isVisibleAscii('~'));

        assertFalse(FileAnalyzer.isVisibleAscii(' '));
        assertFalse(FileAnalyzer.isVisibleAscii('\t'));
        assertFalse(FileAnalyzer.isVisibleAscii('\n'));
        assertFalse(FileAnalyzer.isVisibleAscii('\0'));
    }

    /**
     * 测试：isVisibleChar方法（包含中文判断）
     */
    @Test
    public void testIsVisibleChar() {
        assertTrue(FileAnalyzer.isVisibleChar('A'));
        assertTrue(FileAnalyzer.isVisibleChar('5'));

        assertTrue(FileAnalyzer.isVisibleChar('你'));
        assertTrue(FileAnalyzer.isVisibleChar('好'));
        assertTrue(FileAnalyzer.isVisibleChar('中'));

        assertFalse(FileAnalyzer.isVisibleChar(' '));
        assertFalse(FileAnalyzer.isVisibleChar('\n'));
    }

    /**
     * 辅助方法：将文本写入文件（UTF-8编码）
     */
    private void writeTextFile(File file, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
