package com.netscan;

import org.junit.After;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * HTML报告生成器测试
 */
public class ReportGeneratorTest {

    private static final String TEST_REPORT_DIR = System.getProperty("java.io.tmpdir")
            + File.separator + "netscan_test_report";

    @After
    public void tearDown() {
        // 清理临时报告目录
        File dir = new File(TEST_REPORT_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    /**
     * 测试HTML构建包含必要元素
     */
    @Test
    public void testBuildHtmlContainsBasicStructure() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("nmap result", "data access result", "datanode result");

        assertTrue("应包含DOCTYPE", html.contains("<!DOCTYPE html>"));
        assertTrue("应包含html标签", html.contains("<html"));
        assertTrue("应包含head标签", html.contains("<head>"));
        assertTrue("应包含body标签", html.contains("<body>"));
        assertTrue("应包含标题", html.contains("NetScan"));
    }

    /**
     * 测试HTML构建包含三个扫描结果
     */
    @Test
    public void testBuildHtmlContainsResults() {
        ReportGenerator generator = new ReportGenerator();
        // 使用合并后的 nmap 格式
        String nmapResult = "=== 192.168.1.1 ===\n"
                + "Discovered open port 3306/tcp on 192.168.1.1\n"
                + "3306/tcp  open   mysql\n"
                + "--";
        // 使用合并格式的数据访问结果
        String dataAccessResult = "=== 192.168.1.1 > 192.168.1.2 ===\n"
                + ">>> 192.168.1.1.3306 > 192.168.1.2.50000\n"
                + "SELECT * FROM users\n"
                + "--";
        String datanodeResult = "192.168.1.1.50016 > 192.168.1.2.3306";

        String html = generator.buildHtml(nmapResult, dataAccessResult, datanodeResult);

        assertTrue("应包含nmap结果", html.contains("Discovered open port 3306/tcp on 192.168.1.1"));
        assertTrue("应包含数据访问SQL", html.contains("SELECT * FROM users"));
        assertTrue("应包含数据节点结果", html.contains("192.168.1.1.50016"));
    }

    /**
     * 测试HTML特殊字符转义
     */
    @Test
    public void testEscapeHtml() {
        assertEquals("应转义&号", "&amp;", ReportGenerator.escapeHtml("&"));
        assertEquals("应转义<号", "&lt;", ReportGenerator.escapeHtml("<"));
        assertEquals("应转义>号", "&gt;", ReportGenerator.escapeHtml(">"));
        assertEquals("应转义双引号", "&quot;", ReportGenerator.escapeHtml("\""));
        assertEquals("应转义单引号", "&#39;", ReportGenerator.escapeHtml("'"));
    }

    /**
     * 测试null输入转义
     */
    @Test
    public void testEscapeHtmlNull() {
        assertEquals("null应返回空字符串", "", ReportGenerator.escapeHtml(null));
    }

    /**
     * 测试HTML包含包含特殊字符的内容
     */
    @Test
    public void testBuildHtmlEscapesSpecialChars() {
        ReportGenerator generator = new ReportGenerator();
        // nmap 区域现在是折叠格式，特殊字符放到内容行中测试转义
        String nmapWithXss = "=== 10.0.0.1 ===\n"
                + "<script>alert('xss')</script>\n"
                + "--\n";
        String html = generator.buildHtml(nmapWithXss, "", "");

        // 确保特殊字符被转义
        assertFalse("不应包含未转义的script标签", html.contains("<script>alert"));
        assertTrue("应包含转义后的script标签", html.contains("&lt;script&gt;"));
    }

    /**
     * 测试空结果生成报告
     */
    @Test
    public void testBuildHtmlWithEmptyResults() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("", "", "");

        assertTrue("空结果也应生成有效HTML", html.contains("<!DOCTYPE html>"));
        assertTrue("应包含nmap区域标题", html.contains("nmap"));
        assertTrue("应包含tcpdump区域标题", html.contains("tcpdump"));
    }

    /**
     * 测试报告文件生成
     */
    @Test
    public void testGenerateReport() throws IOException {
        ReportGenerator generator = new ReportGenerator();
        String reportPath = TEST_REPORT_DIR + File.separator + "test_report.html";

        // 捕获System.out输出
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            generator.generateReport(reportPath,
                    "=== 10.0.0.1 ===\nDiscovered open port 80/tcp on 10.0.0.1\n80/tcp  open   http\n--",
                    "=== 10.0.0.1 > 10.0.0.2 ===\n>>> 10.0.0.1.3306 > 10.0.0.2.50000\ndata access output\n--",
                    "datanode output");
        } finally {
            System.setOut(originalOut);
        }

        // 验证文件已创建
        File reportFile = new File(reportPath);
        assertTrue("报告文件应已创建", reportFile.exists());

        // 读取并验证文件内容
        String content = new String(Files.readAllBytes(reportFile.toPath()), "UTF-8");
        assertTrue("文件应包含HTML内容", content.contains("<!DOCTYPE html>"));
        assertTrue("文件应包含nmap输出", content.contains("Discovered open port 80/tcp on 10.0.0.1"));
        assertTrue("文件应包含数据访问输出", content.contains("data access output"));
        assertTrue("文件应包含数据节点输出", content.contains("datanode output"));
    }

    /**
     * 测试报告文件生成时自动创建目录
     */
    @Test
    public void testGenerateReportCreatesDirectory() throws IOException {
        ReportGenerator generator = new ReportGenerator();
        String deepPath = TEST_REPORT_DIR + File.separator + "deep" + File.separator + "path"
                + File.separator + "report.html";

        // 捕获System.out输出
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));

        try {
            generator.generateReport(deepPath, "", "", "");
        } finally {
            System.setOut(originalOut);
        }

        File reportFile = new File(deepPath);
        assertTrue("深层目录下的报告文件应已创建", reportFile.exists());

        // 清理深层目录
        reportFile.delete();
        new File(reportFile.getParent()).delete();
        new File(new File(reportFile.getParent()).getParent()).delete();
    }

    /**
     * 测试HTML包含CSS样式
     */
    @Test
    public void testBuildHtmlContainsStyles() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("", "", "");

        assertTrue("应包含style标签", html.contains("<style>"));
        assertTrue("应包含CSS样式", html.contains("font-family"));
    }

    /**
     * 测试HTML包含时间戳
     */
    @Test
    public void testBuildHtmlContainsTimestamp() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("", "", "");

        assertTrue("应包含生成时间", html.contains("生成时间"));
    }

    // ======================== buildDataAccessHtml 折叠功能测试 ========================

    /**
     * 测试折叠HTML包含details/summary标签
     */
    @Test
    public void testBuildDataAccessHtml_containsDetailsSummary() {
        ReportGenerator generator = new ReportGenerator();
        String input = "=== 192.168.48.1 > 192.168.48.204 ===\n"
                + ">>> 192.168.48.1.50551 > 192.168.48.204.3306\n"
                + "SELECT * FROM users\n"
                + "--\n";
        String result = generator.buildDataAccessHtml(input);

        assertTrue("应包含details标签", result.contains("<details>"));
        assertTrue("应包含summary标签", result.contains("<summary>"));
        // summary中显示 IP > IP
        assertTrue("应包含IP分组", result.contains("192.168.48.1"));
        // 展开内容中应包含原始带端口的连接对
        assertTrue("应包含原始连接对", result.contains("192.168.48.1.50551"));
    }

    /**
     * 测试折叠HTML中SQL详情带序号
     */
    @Test
    public void testBuildDataAccessHtml_sqlWithLineNumbers() {
        ReportGenerator generator = new ReportGenerator();
        String input = "=== 10.0.0.1 > 10.0.0.2 ===\n"
                + ">>> 10.0.0.1.3306 > 10.0.0.2.50000\n"
                + "SELECT * FROM t1\n"
                + "INSERT INTO t2 VALUES(1)\n"
                + "UPDATE t3 SET a=1\n"
                + "--\n";
        String result = generator.buildDataAccessHtml(input);

        // 验证序号（只对SQL行编号，不包含 >>> 子头行）
        assertTrue("应包含序号1", result.contains("1.</span>"));
        assertTrue("应包含序号2", result.contains("2.</span>"));
        assertTrue("应包含序号3", result.contains("3.</span>"));
        // 验证SQL内容
        assertTrue("应包含第1条SQL", result.contains("SELECT * FROM t1"));
        assertTrue("应包含第2条SQL", result.contains("INSERT INTO t2"));
        assertTrue("应包含第3条SQL", result.contains("UPDATE t3"));
        // 验证子连接头行带特殊样式
        assertTrue("应包含子连接头样式", result.contains("sub-conn"));
    }

    /**
     * 测试多个连接对分组折叠
     */
    @Test
    public void testBuildDataAccessHtml_multipleGroups() {
        ReportGenerator generator = new ReportGenerator();
        String input = "=== 10.0.0.1 > 10.0.0.2 ===\n"
                + ">>> 10.0.0.1.3306 > 10.0.0.2.50000\n"
                + "SQL_A\n"
                + "--\n"
                + "=== 10.0.0.3 > 10.0.0.4 ===\n"
                + ">>> 10.0.0.3.3306 > 10.0.0.4.50000\n"
                + "SQL_C1\n"
                + "SQL_C2\n"
                + "--\n";
        String result = generator.buildDataAccessHtml(input);

        // 应有2个details块
        int detailsCount = result.split("<details>").length - 1;
        assertEquals("应有2个折叠块", 2, detailsCount);

        // 验证SQL条数显示（不含 >>> 子头行）
        assertTrue("第一组应显示1条", result.contains("1 条SQL"));
        assertTrue("第二组应显示2条", result.contains("2 条SQL"));
    }

    /**
     * 测试空数据访问结果
     */
    @Test
    public void testBuildDataAccessHtml_empty() {
        ReportGenerator generator = new ReportGenerator();
        String result = generator.buildDataAccessHtml("");
        assertTrue("空结果应显示无数据", result.contains("无数据"));

        String resultNull = generator.buildDataAccessHtml(null);
        assertTrue("null结果应显示无数据", resultNull.contains("无数据"));
    }

    /**
     * 测试折叠HTML中特殊字符被转义
     */
    @Test
    public void testBuildDataAccessHtml_escapesSpecialChars() {
        ReportGenerator generator = new ReportGenerator();
        String input = "=== 10.0.0.1 > 10.0.0.2 ===\n"
                + ">>> 10.0.0.1.3306 > 10.0.0.2.50000\n"
                + "SELECT * FROM t WHERE a<10 AND b>5\n"
                + "--\n";
        String result = generator.buildDataAccessHtml(input);

        // SQL中的<和>应被转义
        assertTrue("SQL中<应被转义", result.contains("a&lt;10"));
        assertTrue("SQL中>应被转义", result.contains("b&gt;5"));
    }

    /**
     * 测试完整buildHtml中第二部分使用折叠结构
     */
    @Test
    public void testBuildHtml_dataAccessSection_usesFolding() {
        ReportGenerator generator = new ReportGenerator();
        String dataAccess = "=== 192.168.48.1 > 192.168.48.204 ===\n"
                + ">>> 192.168.48.1.50551 > 192.168.48.204.3306\n"
                + "SELECT COUNT(*) FROM information_schema.TABLES\n"
                + "--\n";
        String html = generator.buildHtml("nmap", dataAccess, "datanode");

        assertTrue("报告应包含details折叠", html.contains("<details>"));
        assertTrue("报告应包含summary", html.contains("<summary>"));
        assertTrue("报告应包含折叠样式", html.contains("access-groups"));
    }

    // ======================== buildNmapHtml 折叠功能测试 ========================

    /**
     * 测试 nmap 折叠HTML包含details/summary标签
     */
    @Test
    public void testBuildNmapHtml_containsDetailsSummary() {
        ReportGenerator generator = new ReportGenerator();
        String input = "=== 192.168.48.204 ===\n"
                + "Discovered open port 3306/tcp on 192.168.48.204\n"
                + "3306/tcp  open   mysql\n"
                + "--\n";
        String result = generator.buildNmapHtml(input);

        assertTrue("应包含details标签", result.contains("<details>"));
        assertTrue("应包含summary标签", result.contains("<summary>"));
        assertTrue("应包含IP地址", result.contains("192.168.48.204"));
        assertTrue("应包含端口计数", result.contains("1 个开放端口"));
    }

    /**
     * 测试 nmap 多个IP分组折叠
     */
    @Test
    public void testBuildNmapHtml_multipleGroups() {
        ReportGenerator generator = new ReportGenerator();
        String input = "=== 127.0.0.1 ===\n"
                + "Discovered open port 8080/tcp on 127.0.0.1\n"
                + "8080/tcp  open   http-proxy\n"
                + "--\n"
                + "=== 192.168.48.204 ===\n"
                + "Discovered open port 3306/tcp on 192.168.48.204\n"
                + "3306/tcp  open   mysql\n"
                + "Discovered open port 8080/tcp on 192.168.48.204\n"
                + "8080/tcp  open   http-proxy\n"
                + "--\n";
        String result = generator.buildNmapHtml(input);

        int detailsCount = result.split("<details>").length - 1;
        assertEquals("应有2个折叠块", 2, detailsCount);
        assertTrue("应包含1个开放端口", result.contains("1 个开放端口"));
        assertTrue("应包含2个开放端口", result.contains("2 个开放端口"));
    }

    /**
     * 测试 nmap 空结果
     */
    @Test
    public void testBuildNmapHtml_empty() {
        ReportGenerator generator = new ReportGenerator();
        assertTrue("空结果应显示无数据", generator.buildNmapHtml("").contains("无数据"));
        assertTrue("null结果应显示无数据", generator.buildNmapHtml(null).contains("无数据"));
    }

    /**
     * 测试完整buildHtml中 nmap 部分使用折叠结构
     */
    @Test
    public void testBuildHtml_nmapSection_usesFolding() {
        ReportGenerator generator = new ReportGenerator();
        String nmapMerged = "=== 192.168.48.204 ===\n"
                + "Discovered open port 3306/tcp on 192.168.48.204\n"
                + "3306/tcp  open   mysql\n"
                + "--\n";
        String html = generator.buildHtml(nmapMerged, "", "");

        assertTrue("报告应包含nmap折叠", html.contains("nmap-groups"));
        assertTrue("报告应包含details", html.contains("<details>"));
        assertTrue("报告应包含IP", html.contains("192.168.48.204"));
    }

    // ======================== buildNodesHtml 节点模块测试 ========================

    /**
     * 测试节点模块包含IP地址和计数
     */
    @Test
    public void testBuildNodesHtml_containsIpsAndCount() {
        ReportGenerator generator = new ReportGenerator();
        Set<String> ips = new TreeSet<String>();
        ips.add("10.0.0.1");
        ips.add("192.168.48.204");

        String result = generator.buildNodesHtml(ips);
        assertTrue("应包含IP 10.0.0.1", result.contains("10.0.0.1"));
        assertTrue("应包含IP 192.168.48.204", result.contains("192.168.48.204"));
        assertTrue("应包含节点数 2", result.contains("2 个节点"));
        assertTrue("应包含node-item样式", result.contains("node-item"));
    }

    /**
     * 测试节点模块空结果
     */
    @Test
    public void testBuildNodesHtml_empty() {
        ReportGenerator generator = new ReportGenerator();
        Set<String> empty = new TreeSet<String>();
        assertTrue("空集应显示无数据", generator.buildNodesHtml(empty).contains("无数据"));
        assertTrue("null应显示无数据", generator.buildNodesHtml(null).contains("无数据"));
    }

    /**
     * 测试完整buildHtml中包含节点模块
     */
    @Test
    public void testBuildHtml_containsNodesSection() {
        ReportGenerator generator = new ReportGenerator();
        String nmap = "=== 10.0.0.1 ===\nDiscovered open port 80/tcp on 10.0.0.1\n80/tcp  open   http\n--\n";
        String dataAccess = "=== 10.0.0.1 > 10.0.0.2 ===\n>>> 10.0.0.1.3306 > 10.0.0.2.50000\nSELECT 1\n--\n";
        String datanode = "10.0.0.3 > 10.0.0.4\n";

        String html = generator.buildHtml(nmap, dataAccess, datanode);
        assertTrue("应包含节点标题", html.contains("节点"));
        assertTrue("应包含node-groups样式", html.contains("node-groups"));
        // 应包含从三个模块提取的所有IP
        assertTrue("应包含10.0.0.1", html.contains("10.0.0.1"));
        assertTrue("应包含10.0.0.2", html.contains("10.0.0.2"));
        assertTrue("应包含10.0.0.3", html.contains("10.0.0.3"));
        assertTrue("应包含10.0.0.4", html.contains("10.0.0.4"));
        assertTrue("应包含4个节点", html.contains("4 个节点"));
    }

    /**
     * 测试节点模块在报告中位于三个扫描模块之前
     */
    @Test
    public void testBuildHtml_nodesBeforeOtherSections() {
        ReportGenerator generator = new ReportGenerator();
        String nmap = "=== 10.0.0.1 ===\nport\n--\n";
        String html = generator.buildHtml(nmap, "", "");

        // 使用section标题来定位，避免匹配到CSS中的类名
        int nodesPos = html.indexOf("🌐 节点");
        int nmapPos = html.indexOf("📡 1. 网络节点发现");
        assertTrue("节点模块标题应存在", nodesPos >= 0);
        assertTrue("nmap模块标题应存在", nmapPos >= 0);
        assertTrue("节点模块应在nmap之前", nodesPos < nmapPos);
    }

    // ======================== computeHash 测试 ========================

    /**
     * 测试 computeHash 返回64位十六进制字符串
     */
    @Test
    public void testComputeHash_returnsHexString() {
        String hash = ReportGenerator.computeHash("hello world");
        assertNotNull("哈希不应为null", hash);
        assertEquals("SHA-256哈希应为64字符", 64, hash.length());
        assertTrue("应为十六进制字符串", hash.matches("[0-9a-f]{64}"));
    }

    /**
     * 测试相同输入产生相同哈希
     */
    @Test
    public void testComputeHash_sameInputSameOutput() {
        String hash1 = ReportGenerator.computeHash("test content");
        String hash2 = ReportGenerator.computeHash("test content");
        assertEquals("相同输入应产生相同哈希", hash1, hash2);
    }

    /**
     * 测试不同输入产生不同哈希
     */
    @Test
    public void testComputeHash_differentInputDifferentOutput() {
        String hash1 = ReportGenerator.computeHash("content A");
        String hash2 = ReportGenerator.computeHash("content B");
        assertNotEquals("不同输入应产生不同哈希", hash1, hash2);
    }

    // ======================== 完整性电子标签测试 ========================

    /**
     * 测试生成的HTML包含完整性标签
     */
    @Test
    public void testBuildHtml_containsIntegrityTag() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("", "", "");

        assertTrue("应包含report-content容器", html.contains("id=\"report-content\""));
        assertTrue("应包含data-integrity属性", html.contains("data-integrity=\""));
    }

    /**
     * 测试生成的HTML包含完整性校验脚本
     */
    @Test
    public void testBuildHtml_containsIntegrityScript() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("", "", "");

        assertTrue("应包含完整性校验脚本", html.contains("<script>"));
        assertTrue("应包含sha256校验函数", html.contains("sha256"));
        assertTrue("应包含tampered样式类", html.contains("tampered"));
    }

    /**
     * 测试生成的HTML包含篡改检测CSS样式
     */
    @Test
    public void testBuildHtml_containsTamperedStyles() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("", "", "");

        assertTrue("应包含body.tampered样式", html.contains("body.tampered"));
        assertTrue("应包含文档内容不完整提示文本", html.contains("文档内容不完整"));
    }

    /**
     * 测试data-integrity属性值为有效的64位哈希
     */
    @Test
    public void testBuildHtml_integrityHashIsValid() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("=== 10.0.0.1 ===\nport\n--\n", "", "");

        // 提取data-integrity属性值
        int start = html.indexOf("data-integrity=\"") + "data-integrity=\"".length();
        int end = html.indexOf("\"", start);
        String hash = html.substring(start, end);

        assertEquals("哈希应为64字符", 64, hash.length());
        assertTrue("应为十六进制字符串", hash.matches("[0-9a-f]{64}"));
    }

    // ======================== normalizeForIntegrity 测试 ========================

    /**
     * 测试 normalizeForIntegrity 将 &quot; 还原为 "
     */
    @Test
    public void testNormalizeForIntegrity_quotEntity() {
        String input = "SELECT * FROM t WHERE name=&quot;test&quot;";
        String result = ReportGenerator.normalizeForIntegrity(input);
        assertEquals("SELECT * FROM t WHERE name=\"test\"", result);
    }

    /**
     * 测试 normalizeForIntegrity 将 &#39; 还原为 '
     */
    @Test
    public void testNormalizeForIntegrity_aposEntity() {
        String input = "WHERE a=&#39;value&#39;";
        String result = ReportGenerator.normalizeForIntegrity(input);
        assertEquals("WHERE a='value'", result);
    }

    /**
     * 测试 normalizeForIntegrity 保留 &amp; &lt; &gt;
     */
    @Test
    public void testNormalizeForIntegrity_preservesOtherEntities() {
        String input = "&amp; &lt; &gt;";
        String result = ReportGenerator.normalizeForIntegrity(input);
        assertEquals("&amp; &lt; &gt;", result);
    }

    /**
     * 测试 normalizeForIntegrity null 输入
     */
    @Test
    public void testNormalizeForIntegrity_null() {
        assertEquals("", ReportGenerator.normalizeForIntegrity(null));
    }

    /**
     * 测试 data-integrity 哈希与 report-content 内容的一致性
     * 模拟浏览器 innerHTML 行为验证哈希匹配
     */
    @Test
    public void testBuildHtml_hashMatchesNormalizedContent() {
        ReportGenerator generator = new ReportGenerator();
        String html = generator.buildHtml("=== 10.0.0.1 ===\nport\n--\n",
                "=== 10.0.0.1 > 10.0.0.2 ===\n>>> 10.0.0.1.3306 > 10.0.0.2.50000\nSELECT 1\n--\n",
                "10.0.0.3 > 10.0.0.4");

        // 提取 data-integrity 哈希
        int hashStart = html.indexOf("data-integrity=\"") + "data-integrity=\"".length();
        int hashEnd = html.indexOf("\"", hashStart);
        String storedHash = html.substring(hashStart, hashEnd);

        // 提取 report-content div 的 innerHTML（开始标签之后、结束标签之前的内容）
        String startTag = "data-integrity=\"" + storedHash + "\">";
        int contentStart = html.indexOf(startTag) + startTag.length();
        int contentEnd = html.indexOf("</div>\n    <script>"); // report-content 的结束标签
        String innerHTML = html.substring(contentStart, contentEnd);

        // 模拟浏览器行为：&quot; → " 和 &#39; → '
        String normalized = ReportGenerator.normalizeForIntegrity(innerHTML);

        // 重新计算哈希，应与存储的哈希一致
        String recomputedHash = ReportGenerator.computeHash(normalized);
        assertEquals("哈希应与 innerHTML 内容一致（未篡改时不应触发报警）", storedHash, recomputedHash);
    }
}
