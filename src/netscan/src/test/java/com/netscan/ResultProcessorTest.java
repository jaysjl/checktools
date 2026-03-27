package com.netscan;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * ResultProcessor 单元测试
 * 覆盖记录计数、截断、去源端口、去重等全部逻辑
 */
public class ResultProcessorTest {

    // ======================== removeSourcePort 测试 ========================

    @Test
    public void testRemoveSourcePort_normalLine() {
        String input = "192.168.48.1.50016 > 192.168.48.204.3306";
        String expected = "192.168.48.1 > 192.168.48.204";
        assertEquals(expected, ResultProcessor.removeSourcePort(input));
    }

    @Test
    public void testRemoveSourcePort_differentPort() {
        String input = "10.0.0.1.12345 > 10.0.0.2.8080";
        String expected = "10.0.0.1 > 10.0.0.2";
        assertEquals(expected, ResultProcessor.removeSourcePort(input));
    }

    @Test
    public void testRemoveSourcePort_invalidFormat() {
        // 不符合 IP.port > dest 格式时应返回原始行
        String input = "some random text";
        assertEquals(input, ResultProcessor.removeSourcePort(input));
    }

    @Test
    public void testRemoveSourcePort_highPort() {
        String input = "192.168.1.100.65535 > 172.16.0.1.22";
        String expected = "192.168.1.100 > 172.16.0.1";
        assertEquals(expected, ResultProcessor.removeSourcePort(input));
    }

    // ======================== countDataAccessRecords 测试 ========================

    @Test
    public void testCountDataAccessRecords_normal() {
        String input = "=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
                + "SELECT * FROM t\n"
                + "--\n"
                + "=== 192.168.48.1.50552 > 192.168.48.204.3306 ===\n"
                + "INSERT INTO t\n"
                + "--\n";
        assertEquals(2, ResultProcessor.countDataAccessRecords(input));
    }

    @Test
    public void testCountDataAccessRecords_empty() {
        assertEquals(0, ResultProcessor.countDataAccessRecords(""));
        assertEquals(0, ResultProcessor.countDataAccessRecords(null));
    }

    @Test
    public void testCountDataAccessRecords_noSeparator() {
        String input = "just some text\nno separators here";
        assertEquals(0, ResultProcessor.countDataAccessRecords(input));
    }

    @Test
    public void testCountDataAccessRecords_singleRecord() {
        String input = "=== header ===\ndata\n--\n";
        assertEquals(1, ResultProcessor.countDataAccessRecords(input));
    }

    @Test
    public void testCountDataAccessRecords_separatorWithSpaces() {
        // "--" 前后有空格也应被识别
        String input = "data\n  --  \nmore data\n--\n";
        assertEquals(2, ResultProcessor.countDataAccessRecords(input));
    }

    // ======================== truncateDataAccessResult 测试 ========================

    @Test
    public void testTruncateDataAccessResult_belowLimit() {
        String input = "=== line1 ===\ndata1\n--\n=== line2 ===\ndata2\n--\n";
        // 限制5条，实际只有2条，不截断
        String result = ResultProcessor.truncateDataAccessResult(input, 5);
        assertEquals(2, ResultProcessor.countDataAccessRecords(result));
    }

    @Test
    public void testTruncateDataAccessResult_atLimit() {
        // 构造3条记录，限制3条
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append("=== record ").append(i).append(" ===\n");
            sb.append("data\n");
            sb.append("--\n");
        }
        String result = ResultProcessor.truncateDataAccessResult(sb.toString(), 3);
        assertEquals(3, ResultProcessor.countDataAccessRecords(result));
    }

    @Test
    public void testTruncateDataAccessResult_exceedsLimit() {
        // 构造10条记录，限制3条
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("=== record ").append(i).append(" ===\n");
            sb.append("data\n");
            sb.append("--\n");
        }
        String result = ResultProcessor.truncateDataAccessResult(sb.toString(), 3);
        assertEquals(3, ResultProcessor.countDataAccessRecords(result));
        // 不应包含第4条记录的内容
        assertFalse("不应包含第4条记录", result.contains("record 3"));
    }

    @Test
    public void testTruncateDataAccessResult_empty() {
        assertEquals("", ResultProcessor.truncateDataAccessResult("", 10));
        assertEquals("", ResultProcessor.truncateDataAccessResult(null, 10));
    }

    @Test
    public void testTruncateDataAccessResult_preservesContent() {
        // 验证截断后内容完整性
        String input = "=== header1 ===\nSELECT 1\n--\n=== header2 ===\nSELECT 2\n--\n";
        String result = ResultProcessor.truncateDataAccessResult(input, 1);
        assertTrue("应包含第一条记录", result.contains("SELECT 1"));
        assertFalse("不应包含第二条记录", result.contains("SELECT 2"));
    }

    // ======================== deduplicateDatanodeResult 测试 ========================

    @Test
    public void testDeduplicateDatanodeResult_removeDuplicates() {
        // 两行只有源端口不同，去掉两端端口后相同 → 去重后只保留一条
        String input = "192.168.48.1.50016 > 192.168.48.204.3306\n"
                + "192.168.48.1.50017 > 192.168.48.204.3306\n";
        String result = ResultProcessor.deduplicateDatanodeResult(input, 1000);
        assertEquals("192.168.48.1 > 192.168.48.204", result);
    }

    @Test
    public void testDeduplicateDatanodeResult_keepDifferentDest() {
        // 目标IP不同，应保留两条；目标IP相同但端口不同，去重后只保留一条
        String input = "192.168.48.1.50016 > 192.168.48.204.3306\n"
                + "192.168.48.1.50017 > 192.168.48.205.8080\n";
        String result = ResultProcessor.deduplicateDatanodeResult(input, 1000);
        String[] lines = result.split("\n");
        assertEquals(2, lines.length);
        assertEquals("192.168.48.1 > 192.168.48.204", lines[0]);
        assertEquals("192.168.48.1 > 192.168.48.205", lines[1]);
    }

    @Test
    public void testDeduplicateDatanodeResult_truncateAtLimit() {
        // 构造5条不重复记录，限制3条
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("10.0.0.1.100 > 10.0.0.").append(i).append(".80\n");
        }
        String result = ResultProcessor.deduplicateDatanodeResult(sb.toString(), 3);
        String[] lines = result.split("\n");
        assertEquals(3, lines.length);
    }

    @Test
    public void testDeduplicateDatanodeResult_empty() {
        assertEquals("", ResultProcessor.deduplicateDatanodeResult("", 1000));
        assertEquals("", ResultProcessor.deduplicateDatanodeResult(null, 1000));
    }

    @Test
    public void testDeduplicateDatanodeResult_allDuplicates() {
        // 5行完全相同（去掉源端口后），去重后只有1条
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("192.168.1.1.").append(50000 + i).append(" > 192.168.1.2.3306\n");
        }
        String result = ResultProcessor.deduplicateDatanodeResult(sb.toString(), 1000);
        assertEquals("192.168.1.1 > 192.168.1.2", result);
    }

    @Test
    public void testDeduplicateDatanodeResult_preservesOrder() {
        // 验证去重后保持首次出现的顺序
        String input = "10.0.0.1.100 > 10.0.0.3.80\n"
                + "10.0.0.1.101 > 10.0.0.1.80\n"
                + "10.0.0.1.102 > 10.0.0.2.80\n"
                + "10.0.0.1.103 > 10.0.0.1.80\n"; // 与第2行重复
        String result = ResultProcessor.deduplicateDatanodeResult(input, 1000);
        String[] lines = result.split("\n");
        assertEquals(3, lines.length);
        assertEquals("10.0.0.1 > 10.0.0.3", lines[0]);
        assertEquals("10.0.0.1 > 10.0.0.1", lines[1]);
        assertEquals("10.0.0.1 > 10.0.0.2", lines[2]);
    }

    @Test
    public void testDeduplicateDatanodeResult_skipEmptyLines() {
        String input = "10.0.0.1.100 > 10.0.0.2.80\n\n\n10.0.0.1.101 > 10.0.0.3.80\n";
        String result = ResultProcessor.deduplicateDatanodeResult(input, 1000);
        String[] lines = result.split("\n");
        assertEquals(2, lines.length);
    }

    // ======================== countDeduplicatedDatanodeRecords 测试 ========================

    @Test
    public void testCountDeduplicatedDatanodeRecords_withDuplicates() {
        String input = "192.168.48.1.50016 > 192.168.48.204.3306\n"
                + "192.168.48.1.50017 > 192.168.48.204.3306\n"
                + "192.168.48.1.57788 > 192.168.48.204.3306\n"
                + "192.168.48.1.57788 > 192.168.48.204.3306\n"
                + "192.168.48.1.57789 > 192.168.48.204.8080\n";
        // 去掉源端口和目标端口后：
        // 192.168.48.1 > 192.168.48.204 (出现5次)
        assertEquals(1, ResultProcessor.countDeduplicatedDatanodeRecords(input));
    }

    @Test
    public void testCountDeduplicatedDatanodeRecords_allUnique() {
        String input = "10.0.0.1.100 > 10.0.0.2.80\n"
                + "10.0.0.1.101 > 10.0.0.3.80\n"
                + "10.0.0.1.102 > 10.0.0.4.80\n";
        assertEquals(3, ResultProcessor.countDeduplicatedDatanodeRecords(input));
    }

    @Test
    public void testCountDeduplicatedDatanodeRecords_empty() {
        assertEquals(0, ResultProcessor.countDeduplicatedDatanodeRecords(""));
        assertEquals(0, ResultProcessor.countDeduplicatedDatanodeRecords(null));
    }

    // ======================== 使用 README 示例数据的集成测试 ========================

    @Test
    public void testDataAccessResult_readmeExample() {
        // 来自 README 的示例数据
        String input = "=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
                + ".....SHOW VARIABLES LIKE 'lower_case_%'\n"
                + "--\n"
                + "=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
                + "h....SELECT SCHEMA_NAME\n"
                + "--\n"
                + "=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
                + ".....SELECT COUNT(*)\n"
                + "--\n"
                + "=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
                + ".....SELECT TABLE_SCHEMA\n"
                + "--\n"
                + "=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
                + ".....SELECT TABLE_SCHEMA, TABLE_NAME\n"
                + "--\n"
                + "=== 192.168.48.204.3306 > 192.168.48.1.50551 ===\n"
                + "CLIENT_STATISTICS.ROWS_UPDATED\n"
                + "--\n";

        assertEquals(6, ResultProcessor.countDataAccessRecords(input));

        // 截断到3条
        String truncated = ResultProcessor.truncateDataAccessResult(input, 3);
        assertEquals(3, ResultProcessor.countDataAccessRecords(truncated));
    }

    @Test
    public void testDatanodeResult_readmeExample() {
        // 来自 README 的示例数据
        String input = "192.168.48.1.50016 > 192.168.48.204.3306\n"
                + "192.168.48.1.50017 > 192.168.48.204.3306\n"
                + "192.168.48.1.57788 > 192.168.48.204.3306\n"
                + "192.168.48.1.57788 > 192.168.48.204.3306\n"
                + "192.168.48.1.57789 > 192.168.48.204.3306\n";

        // 全部去掉端口后都变成 192.168.48.1 > 192.168.48.204，去重后只有1条
        String result = ResultProcessor.deduplicateDatanodeResult(input, 1000);
        assertEquals("192.168.48.1 > 192.168.48.204", result);
        assertEquals(1, ResultProcessor.countDeduplicatedDatanodeRecords(input));
    }

    // ======================== 大量数据测试 ========================

    @Test
    public void testTruncateDataAccessResult_1000Records() {
        // 构造1500条记录，限制1000条
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1500; i++) {
            sb.append("=== record ").append(i).append(" ===\n");
            sb.append("SELECT * FROM table_").append(i).append("\n");
            sb.append("--\n");
        }
        String result = ResultProcessor.truncateDataAccessResult(sb.toString(), 1000);
        assertEquals(1000, ResultProcessor.countDataAccessRecords(result));
    }

    @Test
    public void testDeduplicateDatanodeResult_1000UniqueRecords() {
        // 构造1500条不同目标的记录，限制1000条
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1500; i++) {
            int b = (i / 256) % 256;
            int c = i % 256;
            sb.append("10.0.0.1.").append(50000 + i)
                    .append(" > 10.0.").append(b).append(".").append(c).append(".80\n");
        }
        String result = ResultProcessor.deduplicateDatanodeResult(sb.toString(), 1000);
        String[] lines = result.split("\n");
        assertEquals(1000, lines.length);
    }

    @Test
    public void testMaxRecordsConstant() {
        assertEquals("MAX_RECORDS 应为 1000", 1000, ResultProcessor.MAX_RECORDS);
    }

    // ======================== stripAnsi 测试 ========================

    @Test
    public void testStripAnsi_withColorCodes() {
        // 模拟 tcpdump 输出中的 ANSI 颜色码
        String input = "[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===[0m";
        String result = ResultProcessor.stripAnsi(input);
        assertEquals("=== 192.168.48.204.3306 > 192.168.48.1.50551 ===", result);
    }

    @Test
    public void testStripAnsi_withoutColorCodes() {
        String input = "=== 192.168.48.204.3306 > 192.168.48.1.50551 ===";
        String result = ResultProcessor.stripAnsi(input);
        assertEquals(input, result);
    }

    @Test
    public void testStripAnsi_null() {
        assertEquals("", ResultProcessor.stripAnsi(null));
    }

    @Test
    public void testStripAnsi_realEscapeSequence() {
        // 真实的 ANSI ESC 字符 (\033 = \x1b)
        String input = "\u001b[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===\u001b[0m";
        String result = ResultProcessor.stripAnsi(input);
        assertEquals("=== 192.168.48.204.3306 > 192.168.48.1.50551 ===", result);
    }

    // ======================== extractConnection 测试 ========================

    @Test
    public void testExtractConnection_withAnsi() {
        String input = "[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===[0m";
        String result = ResultProcessor.extractConnection(input);
        assertEquals("192.168.48.204.3306 > 192.168.48.1.50551", result);
    }

    @Test
    public void testExtractConnection_withoutAnsi() {
        String input = "=== 192.168.48.204.3306 > 192.168.48.1.52544 ===";
        String result = ResultProcessor.extractConnection(input);
        assertEquals("192.168.48.204.3306 > 192.168.48.1.52544", result);
    }

    @Test
    public void testExtractConnection_null() {
        assertNull(ResultProcessor.extractConnection(null));
    }

    @Test
    public void testExtractConnection_noMatch() {
        // 普通内容行不应匹配
        String input = "SELECT * FROM users";
        assertNull(ResultProcessor.extractConnection(input));
    }

    @Test
    public void testExtractConnection_separator() {
        // 分隔符不应匹配
        assertNull(ResultProcessor.extractConnection("--"));
    }

    @Test
    public void testExtractConnection_realEscapeSequence() {
        String input = "\u001b[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\u001b[0m";
        String result = ResultProcessor.extractConnection(input);
        assertEquals("192.168.48.1.50551 > 192.168.48.204.3306", result);
    }

    // ======================== mergeDataAccessByConnection 测试 ========================

    @Test
    public void testMergeDataAccess_readmeExample() {
        // 使用 README 中的示例数据（用[1;31m格式模拟ANSI码）
        String input =
                "[1;31m=== 192.168.48.204.3306 > 192.168.48.1.52544 ===[0m\n"
              + "some response data\n"
              + "--\n"
              + "[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===\n"
              + ".....SHOW VARIABLES LIKE 'lower_case_%'\n"
              + "--\n"
              + "[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===[0m\n"
              + "h....SELECT SCHEMA_NAME\n"
              + "--\n"
              + "[1;31m=== 192.168.48.1.50551 > 192.168.48.204.3306 ===[0m\n"
              + ".....SELECT COUNT(*)\n"
              + "--\n"
              + "[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===[0m\n"
              + "CLIENT_STATISTICS.ROWS_UPDATED\n"
              + "--\n";

        String result = ResultProcessor.mergeDataAccessByConnection(input);

        // 应该按 IP > IP 合并为2个分组
        // 1) 192.168.48.204 > 192.168.48.1（合并了 .3306>.52544 和 .3306>.50551）
        // 2) 192.168.48.1 > 192.168.48.204（3条合并）
        assertTrue("应包含第一个IP分组", result.contains("=== 192.168.48.204 > 192.168.48.1 ==="));
        assertTrue("应包含第二个IP分组", result.contains("=== 192.168.48.1 > 192.168.48.204 ==="));

        // 原始带端口的连接对应作为 >>> 子头行存在于内容中
        assertTrue("应包含原始连接对子头", result.contains(">>> 192.168.48.204.3306 > 192.168.48.1.52544"));
        assertTrue("应包含原始连接对子头", result.contains(">>> 192.168.48.1.50551 > 192.168.48.204.3306"));
        assertTrue("应包含原始连接对子头", result.contains(">>> 192.168.48.204.3306 > 192.168.48.1.50551"));

        // 合并后的内容应包含所有SQL
        assertTrue("应包含SHOW VARIABLES", result.contains("SHOW VARIABLES"));
        assertTrue("应包含SELECT SCHEMA_NAME", result.contains("SELECT SCHEMA_NAME"));
        assertTrue("应包含SELECT COUNT", result.contains("SELECT COUNT"));
        assertTrue("应包含CLIENT_STATISTICS", result.contains("CLIENT_STATISTICS"));

        // 不应包含ANSI颜色码
        assertFalse("不应包含ANSI码[1;31m", result.contains("[1;31m"));
        assertFalse("不应包含ANSI码[0m", result.contains("[0m"));
    }

    @Test
    public void testMergeDataAccess_sameConnectionMerged() {
        // 3条相同连接对的记录应合并为一组
        String input =
                "=== 10.0.0.1.3306 > 10.0.0.2.50000 ===\n"
              + "SQL_LINE_1\n"
              + "--\n"
              + "=== 10.0.0.1.3306 > 10.0.0.2.50000 ===\n"
              + "SQL_LINE_2\n"
              + "--\n"
              + "=== 10.0.0.1.3306 > 10.0.0.2.50000 ===\n"
              + "SQL_LINE_3\n"
              + "--\n";

        String result = ResultProcessor.mergeDataAccessByConnection(input);

        // 应有一个 IP > IP 分组头
        assertTrue("应包含IP分组头", result.contains("=== 10.0.0.1 > 10.0.0.2 ==="));

        // 原始连接对应作为 >>> 子头行出现3次
        int subHeaderCount = 0;
        for (String line : result.split("\n")) {
            if (line.contains(">>> 10.0.0.1.3306 > 10.0.0.2.50000")) {
                subHeaderCount++;
            }
        }
        assertEquals("原始连接对子头应出现3次", 3, subHeaderCount);

        // 应包含所有3条SQL
        assertTrue(result.contains("SQL_LINE_1"));
        assertTrue(result.contains("SQL_LINE_2"));
        assertTrue(result.contains("SQL_LINE_3"));
    }

    @Test
    public void testMergeDataAccess_differentConnectionsKeptSeparate() {
        String input =
                "=== 10.0.0.1.3306 > 10.0.0.2.50000 ===\n"
              + "SQL_A\n"
              + "--\n"
              + "=== 10.0.0.3.3306 > 10.0.0.4.50000 ===\n"
              + "SQL_C\n"
              + "--\n";

        String result = ResultProcessor.mergeDataAccessByConnection(input);

        // 不同IP应分开
        assertTrue(result.contains("=== 10.0.0.1 > 10.0.0.2 ==="));
        assertTrue(result.contains("=== 10.0.0.3 > 10.0.0.4 ==="));
        // 原始连接对作为子头行
        assertTrue(result.contains(">>> 10.0.0.1.3306 > 10.0.0.2.50000"));
        assertTrue(result.contains(">>> 10.0.0.3.3306 > 10.0.0.4.50000"));
        assertTrue(result.contains("SQL_A"));
        assertTrue(result.contains("SQL_C"));
    }

    @Test
    public void testMergeDataAccess_empty() {
        assertEquals("", ResultProcessor.mergeDataAccessByConnection(""));
        assertEquals("", ResultProcessor.mergeDataAccessByConnection(null));
    }

    @Test
    public void testMergeDataAccess_noSeparatorAtEnd() {
        // 最后一条记录没有 "--" 结尾
        String input =
                "=== 10.0.0.1.3306 > 10.0.0.2.50000 ===\n"
              + "SQL_A\n";

        String result = ResultProcessor.mergeDataAccessByConnection(input);
        assertTrue(result.contains("=== 10.0.0.1 > 10.0.0.2 ==="));
        assertTrue(result.contains(">>> 10.0.0.1.3306 > 10.0.0.2.50000"));
        assertTrue(result.contains("SQL_A"));
    }

    @Test
    public void testMergeDataAccess_preservesOrder() {
        // 验证输出按首次出现顺序排列
        String input =
                "=== 10.0.0.2.3306 > 10.0.0.1.50000 ===\n"
              + "first\n"
              + "--\n"
              + "=== 10.0.0.4.3306 > 10.0.0.3.50000 ===\n"
              + "second\n"
              + "--\n"
              + "=== 10.0.0.2.3306 > 10.0.0.1.50001 ===\n"
              + "third\n"
              + "--\n";

        String result = ResultProcessor.mergeDataAccessByConnection(input);
        int posBA = result.indexOf("=== 10.0.0.2 > 10.0.0.1 ===");
        int posDC = result.indexOf("=== 10.0.0.4 > 10.0.0.3 ===");
        assertTrue("10.0.0.2 > 10.0.0.1 应在 10.0.0.4 > 10.0.0.3 之前", posBA < posDC);
    }

    // ======================== mergeNmapByHost 测试 ========================

    @Test
    public void testMergeNmapByHost_readmeExample() {
        String input = "Discovered open port 8080/tcp on 127.0.0.1\n"
              + "8080/tcp  open   http-proxy\n"
              + "Discovered open port 3306/tcp on 192.168.48.204\n"
              + "3306/tcp  open   mysql\n"
              + "Discovered open port 8080/tcp on 192.168.48.219\n"
              + "8080/tcp  open   http-proxy\n";

        String result = ResultProcessor.mergeNmapByHost(input);

        // 应按IP分为3组
        assertTrue("应包含127.0.0.1分组", result.contains("=== 127.0.0.1 ==="));
        assertTrue("应包含192.168.48.204分组", result.contains("=== 192.168.48.204 ==="));
        assertTrue("应包含192.168.48.219分组", result.contains("=== 192.168.48.219 ==="));

        // 每组内应包含端口详情
        assertTrue("应包含http-proxy", result.contains("http-proxy"));
        assertTrue("应包含mysql", result.contains("mysql"));
    }

    @Test
    public void testMergeNmapByHost_sameIpMerged() {
        // 同一IP多个端口应合并到一个分组
        String input = "Discovered open port 3306/tcp on 192.168.48.204\n"
              + "3306/tcp  open   mysql\n"
              + "Discovered open port 8080/tcp on 192.168.48.204\n"
              + "8080/tcp  open   http-proxy\n";

        String result = ResultProcessor.mergeNmapByHost(input);

        // 只有一个分组头
        int headerCount = 0;
        for (String line : result.split("\n")) {
            if (line.contains("=== 192.168.48.204 ===")) {
                headerCount++;
            }
        }
        assertEquals("同一IP只应出现一次头部", 1, headerCount);

        // 应包含两个端口详情
        assertTrue(result.contains("mysql"));
        assertTrue(result.contains("http-proxy"));
    }

    @Test
    public void testMergeNmapByHost_empty() {
        assertEquals("", ResultProcessor.mergeNmapByHost(""));
        assertEquals("", ResultProcessor.mergeNmapByHost(null));
    }

    @Test
    public void testMergeNmapByHost_preservesOrder() {
        String input = "Discovered open port 80/tcp on 10.0.0.2\n"
              + "80/tcp  open   http\n"
              + "Discovered open port 22/tcp on 10.0.0.1\n"
              + "22/tcp  open   ssh\n";

        String result = ResultProcessor.mergeNmapByHost(input);
        int pos1 = result.indexOf("=== 10.0.0.2 ===");
        int pos2 = result.indexOf("=== 10.0.0.1 ===");
        assertTrue("10.0.0.2 应在 10.0.0.1 之前（按首次出现顺序）", pos1 < pos2);
    }

    // ======================== extractAllIps 测试 ========================

    @Test
    public void testExtractAllIps_fromNmap() {
        String nmap = "=== 192.168.48.204 ===\n"
              + "Discovered open port 3306/tcp on 192.168.48.204\n"
              + "--\n"
              + "=== 10.0.0.1 ===\n"
              + "Discovered open port 80/tcp on 10.0.0.1\n"
              + "--\n";
        Set<String> ips = ResultProcessor.extractAllIps(nmap, "", "");
        assertTrue("应包含192.168.48.204", ips.contains("192.168.48.204"));
        assertTrue("应包含10.0.0.1", ips.contains("10.0.0.1"));
        assertEquals("应有2个IP", 2, ips.size());
    }

    @Test
    public void testExtractAllIps_fromDataAccess() {
        String dataAccess = "=== 192.168.48.1 > 192.168.48.204 ===\n"
              + ">>> 192.168.48.1.50551 > 192.168.48.204.3306\n"
              + "SELECT 1\n"
              + "--\n"
              + "=== 10.0.0.1 > 10.0.0.2 ===\n"
              + ">>> 10.0.0.1.3306 > 10.0.0.2.50000\n"
              + "SELECT 2\n"
              + "--\n";
        Set<String> ips = ResultProcessor.extractAllIps("", dataAccess, "");
        assertTrue(ips.contains("192.168.48.1"));
        assertTrue(ips.contains("192.168.48.204"));
        assertTrue(ips.contains("10.0.0.1"));
        assertTrue(ips.contains("10.0.0.2"));
        assertEquals("应有4个IP", 4, ips.size());
    }

    @Test
    public void testExtractAllIps_fromDatanode() {
        String datanode = "192.168.48.1 > 192.168.48.204\n"
              + "10.0.0.1 > 10.0.0.3\n";
        Set<String> ips = ResultProcessor.extractAllIps("", "", datanode);
        assertTrue(ips.contains("192.168.48.1"));
        assertTrue(ips.contains("192.168.48.204"));
        assertTrue(ips.contains("10.0.0.1"));
        assertTrue(ips.contains("10.0.0.3"));
        assertEquals("应有4个IP", 4, ips.size());
    }

    @Test
    public void testExtractAllIps_allModulesMerged() {
        String nmap = "=== 10.0.0.1 ===\nDiscovered open port 80/tcp on 10.0.0.1\n--\n";
        String dataAccess = "=== 10.0.0.1 > 10.0.0.2 ===\n>>> 10.0.0.1.3306 > 10.0.0.2.50000\nSQL\n--\n";
        String datanode = "10.0.0.3 > 10.0.0.4\n";

        Set<String> ips = ResultProcessor.extractAllIps(nmap, dataAccess, datanode);
        assertEquals("应有4个不同IP", 4, ips.size());
        assertTrue(ips.contains("10.0.0.1"));
        assertTrue(ips.contains("10.0.0.2"));
        assertTrue(ips.contains("10.0.0.3"));
        assertTrue(ips.contains("10.0.0.4"));
    }

    @Test
    public void testExtractAllIps_deduplicatesAcrossModules() {
        // 同一个IP在多个模块中都出现，应去重
        String nmap = "=== 192.168.1.1 ===\nport info\n--\n";
        String dataAccess = "=== 192.168.1.1 > 192.168.1.2 ===\n>>> 192.168.1.1.3306 > 192.168.1.2.50000\nSQL\n--\n";
        String datanode = "192.168.1.2 > 192.168.1.1\n";

        Set<String> ips = ResultProcessor.extractAllIps(nmap, dataAccess, datanode);
        assertEquals("重复IP应去重，只有2个", 2, ips.size());
        assertTrue(ips.contains("192.168.1.1"));
        assertTrue(ips.contains("192.168.1.2"));
    }

    @Test
    public void testExtractAllIps_empty() {
        Set<String> ips = ResultProcessor.extractAllIps("", "", "");
        assertTrue("全部为空应返回空集合", ips.isEmpty());
    }

    @Test
    public void testExtractAllIps_null() {
        Set<String> ips = ResultProcessor.extractAllIps(null, null, null);
        assertTrue("全部为null应返回空集合", ips.isEmpty());
    }

    @Test
    public void testExtractAllIps_sorted() {
        String nmap = "=== 192.168.1.1 ===\nport\n--\n"
              + "=== 10.0.0.1 ===\nport\n--\n";
        Set<String> ips = ResultProcessor.extractAllIps(nmap, "", "");
        // TreeSet 按字典序排列，10.0.0.1 < 192.168.1.1
        String first = ips.iterator().next();
        assertEquals("字典序排列，10.0.0.1 在前", "10.0.0.1", first);
    }
}
