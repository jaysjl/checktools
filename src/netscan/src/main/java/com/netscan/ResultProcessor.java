package com.netscan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脚本执行结果处理工具
 * 负责对 tcpdump 脚本输出进行条数统计、去重、截断等操作
 */
public class ResultProcessor {

    /** 默认最大记录数 */
    public static final int MAX_RECORDS = 1000;

    /** 匹配ANSI转义码的正则，用于清除颜色代码 */
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m|\\[\\d+;?\\d*m");

    /** 匹配 === connection_pair === 头行格式的正则 */
    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^\\s*=+\\s*(.+?\\s*>\\s*.+?)\\s*=+\\s*$");

    /** 匹配 IP.port > IP.port 格式的正则：a.b.c.d.port > a.b.c.d.port */
    private static final Pattern IP_PORT_PATTERN = Pattern.compile(
            "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.\\d+\\s*>\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)\\.\\d+");

    /** 匹配 "Discovered open port XXXX/tcp on IP" 格式的正则 */
    private static final Pattern NMAP_DISCOVERED_PATTERN = Pattern.compile(
            "^Discovered\\s+open\\s+port\\s+.+\\s+on\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s*$");

    /**
     * 统计 data_access 输出中的记录条数。
     * 每条记录以单独一行 "--" 作为结束分隔符。
     *
     * @param rawOutput 原始输出
     * @return 记录条数
     */
    public static int countDataAccessRecords(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String line : rawOutput.split("\n")) {
            if ("--".equals(line.trim())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 截断 data_access_discovery_by_tcpdump.sh 的输出：
     * 按 "--" 分隔符分块，每一块算一条记录，达到 maxRecords 条则截断。
     *
     * @param rawOutput  原始输出
     * @param maxRecords 最大记录条数
     * @return 截断后的输出文本
     */
    public static String truncateDataAccessResult(String rawOutput, int maxRecords) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return "";
        }

        String[] lines = rawOutput.split("\n");
        StringBuilder result = new StringBuilder();
        int recordCount = 0;

        for (String line : lines) {
            result.append(line).append("\n");
            // 遇到 "--" 分隔符表示一条记录结束
            if ("--".equals(line.trim())) {
                recordCount++;
                if (recordCount >= maxRecords) {
                    break;
                }
            }
        }

        return result.toString().trim();
    }

    /**
     * 处理 datanode_discovery_by_tcpdump.sh 的输出：
     * 提取源IP和目标IP（去掉两端端口）后去重，去重后达到 maxRecords 条则截断。
     * <p>
     * 输入格式示例：192.168.48.1.50016 > 192.168.48.204.3306
     * 提取后：192.168.48.1 > 192.168.48.204
     * 以提取后的字符串做去重key。
     *
     * @param rawOutput  原始输出
     * @param maxRecords 最大记录条数
     * @return 去重截断后的输出文本（每行为忽略源端口后的结果）
     */
    public static String deduplicateDatanodeResult(String rawOutput, int maxRecords) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return "";
        }

        // 用 LinkedHashSet 去重并保持插入顺序
        Set<String> seen = new LinkedHashSet<String>();
        List<String> deduplicated = new ArrayList<String>();

        for (String line : rawOutput.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 忽略源端口
            String normalized = removeSourcePort(trimmed);
            if (seen.add(normalized)) {
                deduplicated.add(normalized);
                if (deduplicated.size() >= maxRecords) {
                    break;
                }
            }
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < deduplicated.size(); i++) {
            if (i > 0) {
                result.append("\n");
            }
            result.append(deduplicated.get(i));
        }
        return result.toString();
    }

    /**
     * 统计去重后的 datanode 记录条数（用于实时判断是否达到阈值）
     *
     * @param rawOutput 原始输出
     * @return 去重后的记录条数
     */
    public static int countDeduplicatedDatanodeRecords(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return 0;
        }
        Set<String> seen = new LinkedHashSet<String>();
        for (String line : rawOutput.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                seen.add(removeSourcePort(trimmed));
            }
        }
        return seen.size();
    }

    /**
     * 从一行中提取源IP和目标IP，去掉两端的端口号。
     * 输入: "192.168.48.1.50016 > 192.168.48.204.3306"
     * 输出: "192.168.48.1 > 192.168.48.204"
     * <p>
     * 格式：a.b.c.d.port > a.b.c.d.port，去掉两端最后的 .port 部分
     *
     * @param line 原始行
     * @return 去掉端口后的 IP > IP 格式行
     */
    static String removeSourcePort(String line) {
        Matcher matcher = IP_PORT_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1) + " > " + matcher.group(2);
        }
        // 无法匹配时返回原始行
        return line;
    }

    /**
     * 清除字符串中的ANSI转义码（如颜色控制字符）。
     * 例如: "[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===[0m"
     * 返回: "=== 192.168.48.204.3306 > 192.168.48.1.50551 ==="
     *
     * @param text 含ANSI转义码的原始文本
     * @return 清除后的纯文本
     */
    public static String stripAnsi(String text) {
        if (text == null) {
            return "";
        }
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 从一行头信息中提取连接对。
     * 先清除ANSI转义码，再从 "=== IP.PORT > IP.PORT ===" 中提取 "IP.PORT > IP.PORT"。
     * <p>
     * 输入: "[1;31m=== 192.168.48.204.3306 > 192.168.48.1.50551 ===[0m"
     * 输出: "192.168.48.204.3306 > 192.168.48.1.50551"
     *
     * @param headerLine 原始头行
     * @return 提取的连接对字符串；无法匹配时返回null
     */
    public static String extractConnection(String headerLine) {
        if (headerLine == null) {
            return null;
        }
        // 先清除ANSI转义码
        String clean = stripAnsi(headerLine).trim();
        // 尝试匹配 === ... > ... === 格式
        Matcher matcher = HEADER_PATTERN.matcher(clean);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 将 data_access_discovery_by_tcpdump.sh 的输出按 IP > IP 分组合并。
     * <p>
     * 处理流程：
     * 1. 按 "--" 分隔符将输出拆分成多条记录
     * 2. 从每条记录的头行（=== IP.PORT > IP.PORT ===）提取连接对
     * 3. 对连接对去掉两端端口，得到 IP > IP 作为分组 key
     * 4. 将相同 IP > IP 的记录内容合并，原始带端口的连接对以 ">>> IP.PORT > IP.PORT" 格式作为子头行保留在内容中
     * 5. 输出格式：每个 IP > IP 一个分组，组内包含所有原始连接头和SQL内容
     *
     * @param rawOutput 原始脚本输出
     * @return 按 IP > IP 合并后的文本
     */
    public static String mergeDataAccessByConnection(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return "";
        }

        // 用 LinkedHashMap 保持首次出现的顺序
        // key: IP > IP（去掉端口后的连接对）
        // value: 该分组下的所有内容行列表（包含 ">>> IP.PORT > IP.PORT" 子头行）
        Map<String, List<String>> groups = new LinkedHashMap<String, List<String>>();

        String[] lines = rawOutput.split("\n");
        String currentConnection = null;   // 原始连接对: IP.PORT > IP.PORT
        String currentGroupKey = null;     // 分组key: IP > IP
        List<String> currentContent = new ArrayList<String>();

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳过空行
            if (trimmed.isEmpty()) {
                continue;
            }

            // 遇到 "--" 分隔符，保存当前记录
            if ("--".equals(trimmed)) {
                if (currentGroupKey != null) {
                    if (!groups.containsKey(currentGroupKey)) {
                        groups.put(currentGroupKey, new ArrayList<String>());
                    }
                    // 将原始连接对作为子头行加入内容
                    groups.get(currentGroupKey).add(">>> " + currentConnection);
                    groups.get(currentGroupKey).addAll(currentContent);
                }
                currentContent = new ArrayList<String>();
                continue;
            }

            // 尝试从当前行提取连接对（头行）
            String conn = extractConnection(trimmed);
            if (conn != null) {
                currentConnection = conn;
                // 去掉两端端口得到 IP > IP 作为分组key
                currentGroupKey = removeSourcePort(conn);
            } else {
                // 非头行，作为内容行收集
                currentContent.add(trimmed);
            }
        }

        // 处理最后一条记录（可能没有以 "--" 结尾）
        if (currentGroupKey != null && !currentContent.isEmpty()) {
            if (!groups.containsKey(currentGroupKey)) {
                groups.put(currentGroupKey, new ArrayList<String>());
            }
            groups.get(currentGroupKey).add(">>> " + currentConnection);
            groups.get(currentGroupKey).addAll(currentContent);
        }

        // 构建输出：每个 IP > IP 一组
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append("=== ").append(entry.getKey()).append(" ===").append("\n");
            for (String contentLine : entry.getValue()) {
                result.append(contentLine).append("\n");
            }
            result.append("--");
        }

        return result.toString();
    }

    /**
     * 将 datanode_discovery_by_nmap.sh 的输出按主机IP分组合并。
     * <p>
     * 输入格式示例：
     * Discovered open port 8080/tcp on 127.0.0.1
     * 8080/tcp  open   http-proxy
     * Discovered open port 3306/tcp on 192.168.48.204
     * 3306/tcp  open   mysql
     * <p>
     * 处理流程：
     * 1. 遇到 "Discovered open port ... on IP" 行，提取 IP 作为分组 key
     * 2. 后续的端口详情行归入当前 IP 分组
     * 3. 相同 IP 的记录合并到同一分组
     * <p>
     * 输出格式：
     * === 127.0.0.1 ===
     * Discovered open port 8080/tcp on 127.0.0.1
     * 8080/tcp  open   http-proxy
     * --
     *
     * @param rawOutput 原始 nmap 脚本输出
     * @return 按主机IP分组后的文本
     */
    public static String mergeNmapByHost(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) {
            return "";
        }

        // LinkedHashMap 保持首次出现顺序
        // key: IP地址, value: 该IP下所有输出行
        Map<String, List<String>> groups = new LinkedHashMap<String, List<String>>();
        String currentIp = null;

        for (String line : rawOutput.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 尝试匹配 "Discovered open port ... on IP"
            Matcher matcher = NMAP_DISCOVERED_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                currentIp = matcher.group(1);
                if (!groups.containsKey(currentIp)) {
                    groups.put(currentIp, new ArrayList<String>());
                }
                groups.get(currentIp).add(trimmed);
            } else if (currentIp != null) {
                // 端口详情行，归入当前IP分组
                groups.get(currentIp).add(trimmed);
            }
        }

        // 构建输出
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append("=== ").append(entry.getKey()).append(" ===").append("\n");
            for (String contentLine : entry.getValue()) {
                result.append(contentLine).append("\n");
            }
            result.append("--");
        }

        return result.toString();
    }

    /** 匹配 IP > IP 格式的正则（无端口） */
    private static final Pattern IP_PAIR_PATTERN = Pattern.compile(
            "(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s*>\\s*(\\d+\\.\\d+\\.\\d+\\.\\d+)");

    /** 匹配单独 IP 的正则 */
    private static final Pattern SINGLE_IP_PATTERN = Pattern.compile(
            "\\d+\\.\\d+\\.\\d+\\.\\d+");

    /**
     * 从三个模块的结果中提取所有唯一的IP地址，并按字典序排列返回。
     * <p>
     * 提取规则：
     * - nmap结果（已合并格式）：从 "=== IP ===" 头行中提取IP
     * - data_access结果（已合并格式）：从 "=== IP > IP ===" 头行中提取源IP和目标IP
     * - datanode结果（已去重格式）：从 "IP > IP" 行中提取源IP和目标IP
     *
     * @param nmapResult       合并后的 nmap 结果
     * @param dataAccessResult 合并后的 data_access 结果
     * @param datanodeResult   去重后的 datanode 结果
     * @return 按字典序排列的唯一IP地址集合
     */
    public static Set<String> extractAllIps(String nmapResult, String dataAccessResult,
                                             String datanodeResult) {
        Set<String> ips = new TreeSet<String>();

        // 1. 从 nmap 结果提取IP（=== IP === 头行）
        if (nmapResult != null && !nmapResult.isEmpty()) {
            for (String line : nmapResult.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                    String content = trimmed.replaceAll("^=+\\s*", "").replaceAll("\\s*=+$", "");
                    // 单个IP（nmap分组头）
                    if (content.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        ips.add(content);
                    }
                }
            }
        }

        // 2. 从 data_access 结果提取IP（=== IP > IP === 头行）
        if (dataAccessResult != null && !dataAccessResult.isEmpty()) {
            for (String line : dataAccessResult.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                    String content = trimmed.replaceAll("^=+\\s*", "").replaceAll("\\s*=+$", "");
                    Matcher m = IP_PAIR_PATTERN.matcher(content);
                    if (m.matches()) {
                        ips.add(m.group(1));
                        ips.add(m.group(2));
                    }
                }
            }
        }

        // 3. 从 datanode 结果提取IP（IP > IP 行）
        if (datanodeResult != null && !datanodeResult.isEmpty()) {
            for (String line : datanodeResult.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Matcher m = IP_PAIR_PATTERN.matcher(trimmed);
                if (m.matches()) {
                    ips.add(m.group(1));
                    ips.add(m.group(2));
                }
            }
        }

        return ips;
    }
}
