package com.netscan;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * HTML报告生成器
 * 将三个脚本的扫描结果汇总生成一份美观的HTML报告
 */
public class ReportGenerator {

    /**
     * 生成HTML报告文件
     *
     * @param reportPath       报告输出文件路径
     * @param nmapResult       nmap扫描结果
     * @param dataAccessResult tcpdump数据访问发现结果
     * @param datanodeResult   tcpdump数据节点发现结果
     * @throws IOException 写入文件失败
     */
    public void generateReport(String reportPath, String nmapResult,
                                String dataAccessResult, String datanodeResult) throws IOException {
        String html = buildHtml(nmapResult, dataAccessResult, datanodeResult);

        // 确保报告目录存在
        File reportFile = new File(reportPath);
        File parentDir = reportFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 写入HTML文件
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(reportFile), "UTF-8")) {
            writer.write(html);
        }

        System.out.println("  ✔ 报告已生成！");
    }

    /**
     * 构建HTML报告内容（自动提取IP）
     *
     * @param nmapResult       nmap扫描结果
     * @param dataAccessResult tcpdump数据访问发现结果
     * @param datanodeResult   tcpdump数据节点发现结果
     * @return 完整的HTML字符串
     */
    public String buildHtml(String nmapResult, String dataAccessResult, String datanodeResult) {
        Set<String> allIps = ResultProcessor.extractAllIps(nmapResult, dataAccessResult, datanodeResult);
        return buildHtml(nmapResult, dataAccessResult, datanodeResult, allIps);
    }

    /**
     * 构建HTML报告内容
     *
     * @param nmapResult       nmap扫描结果
     * @param dataAccessResult tcpdump数据访问发现结果
     * @param datanodeResult   tcpdump数据节点发现结果
     * @param allIps           所有唯一IP地址集合
     * @return 完整的HTML字符串
     */
    public String buildHtml(String nmapResult, String dataAccessResult,
                            String datanodeResult, Set<String> allIps) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 先构建报告主体内容（用于计算完整性哈希）
        StringBuilder content = new StringBuilder();

        // 页面头部
        content.append("    <div class=\"header\">\n");
        content.append("        <h1>🔍 NetScan 网络扫描报告</h1>\n");
        content.append("        <p class=\"timestamp\">生成时间：").append(escapeHtml(timestamp)).append("</p>\n");
        content.append("    </div>\n");

        content.append("    <div class=\"container\">\n");

        // 节点汇总：列出所有发现的IP
        content.append("        <div class=\"section\">\n");
        content.append("            <h2>🌐 节点</h2>\n");
        content.append("            <p class=\"desc\">汇总以下三个模块中发现的所有IP地址</p>\n");
        content.append("            <div class=\"node-groups\">\n");
        content.append(buildNodesHtml(allIps));
        content.append("            </div>\n");
        content.append("        </div>\n");

        // 第一部分：nmap结果（折叠展示，按主机IP分组）
        content.append("        <div class=\"section\">\n");
        content.append("            <h2>📡 1. 网络节点发现（nmap）</h2>\n");
        content.append("            <p class=\"desc\">通过nmap扫描发现的开放端口和服务（点击主机IP可展开查看端口详情）</p>\n");
        content.append("            <div class=\"nmap-groups\">\n");
        content.append(buildNmapHtml(nmapResult));
        content.append("            </div>\n");
        content.append("        </div>\n");

        // 第二部分：数据访问发现（折叠展示，按连接对分组）
        content.append("        <div class=\"section\">\n");
        content.append("            <h2>🔎 2. 数据访问发现（tcpdump）</h2>\n");
        content.append("            <p class=\"desc\">通过tcpdump捕获的数据库访问SQL语句（点击连接对可展开查看SQL详情）</p>\n");
        content.append("            <div class=\"access-groups\">\n");
        content.append(buildDataAccessHtml(dataAccessResult));
        content.append("            </div>\n");
        content.append("        </div>\n");

        // 第三部分：数据节点发现
        content.append("        <div class=\"section\">\n");
        content.append("            <h2>🖥 3. 数据节点发现（tcpdump）</h2>\n");
        content.append("            <p class=\"desc\">通过tcpdump发现的数据库连接节点</p>\n");
        content.append("            <pre class=\"output\">").append(escapeHtml(datanodeResult)).append("</pre>\n");
        content.append("        </div>\n");

        content.append("    </div>\n");

        // 页脚
        content.append("    <div class=\"footer\">\n");
        content.append("        <p>NetScan v1.0.0 | 网络主机节点发现工具</p>\n");
        content.append("    </div>\n");

        String contentStr = content.toString();
        // 计算报告主体内容的 SHA-256 哈希
        // 需要与浏览器 innerHTML 行为保持一致：
        // 1. innerHTML 包含 opening div tag 后面的换行符 \n
        // 2. 浏览器将文本内容中的 &quot; 还原为 "，将 &#39; 还原为 '
        String normalizedForHash = "\n" + contentStr;
        normalizedForHash = normalizedForHash.replace("&quot;", "\"").replace("&#39;", "'");
        String hash = computeHash(normalizedForHash);

        // 组装完整 HTML
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>NetScan 网络扫描报告</title>\n");
        html.append("    <style>\n");
        html.append(getStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // 报告内容容器，带完整性哈希标签
        html.append("<div id=\"report-content\" data-integrity=\"").append(hash).append("\">\n");
        html.append(contentStr);
        html.append("</div>\n");

        // 完整性校验脚本
        html.append(getIntegrityScript());

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * CSS样式定义
     *
     * @return CSS样式字符串
     */
    private String getStyles() {
        return "        * { margin: 0; padding: 0; box-sizing: border-box; }\n"
             + "        body { font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; "
             + "background: #f5f7fa; color: #333; }\n"
             + "        .header { background: linear-gradient(135deg, #1a73e8, #0d47a1); "
             + "color: white; padding: 30px 40px; text-align: center; }\n"
             + "        .header h1 { font-size: 28px; margin-bottom: 8px; }\n"
             + "        .header .timestamp { font-size: 14px; opacity: 0.85; }\n"
             + "        .container { max-width: 1100px; margin: 30px auto; padding: 0 20px; }\n"
             + "        .section { background: white; border-radius: 8px; "
             + "box-shadow: 0 2px 8px rgba(0,0,0,0.1); margin-bottom: 24px; overflow: hidden; }\n"
             + "        .section h2 { background: #f8f9fa; padding: 16px 24px; "
             + "font-size: 18px; border-bottom: 1px solid #e9ecef; }\n"
             + "        .section .desc { padding: 12px 24px 0; color: #666; font-size: 14px; }\n"
             + "        .section .output { padding: 16px 24px 20px; font-family: "
             + "'Consolas', 'Monaco', 'Courier New', monospace; font-size: 13px; "
             + "line-height: 1.6; background: #1e1e1e; color: #d4d4d4; "
             + "margin: 12px 24px 20px; border-radius: 6px; overflow-x: auto; "
             + "white-space: pre-wrap; word-wrap: break-word; }\n"
             + "        .footer { text-align: center; padding: 20px; color: #999; "
             + "font-size: 13px; }\n"
             + "        .access-groups { padding: 12px 24px 20px; }\n"
             + "        .access-groups details { margin-bottom: 8px; border: 1px solid #e0e0e0; "
             + "border-radius: 6px; overflow: hidden; }\n"
             + "        .access-groups summary { padding: 10px 16px; cursor: pointer; "
             + "background: #f0f4f8; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; "
             + "font-size: 13px; font-weight: bold; color: #1a73e8; user-select: none; "
             + "list-style: none; display: flex; align-items: center; }\n"
             + "        .access-groups summary::-webkit-details-marker { display: none; }\n"
             + "        .access-groups summary::before { content: '▶'; margin-right: 8px; "
             + "font-size: 11px; transition: transform 0.2s; display: inline-block; }\n"
             + "        .access-groups details[open] summary::before { transform: rotate(90deg); }\n"
             + "        .access-groups summary:hover { background: #e3eaf5; }\n"
             + "        .access-groups .sql-detail { background: #1e1e1e; color: #d4d4d4; "
             + "padding: 12px 16px; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; "
             + "font-size: 13px; line-height: 1.6; overflow-x: auto; }\n"
             + "        .access-groups .sql-line { padding: 3px 0; display: flex; }\n"
             + "        .access-groups .sql-no { color: #858585; min-width: 40px; "
             + "text-align: right; margin-right: 12px; flex-shrink: 0; }\n"
             + "        .access-groups .sql-text { white-space: pre-wrap; word-wrap: break-word; }\n"
             + "        .access-groups .sql-count { margin-left: auto; color: #888; "
             + "font-weight: normal; font-size: 12px; }\n"
             + "        .access-groups .sub-conn { padding: 6px 0 2px; color: #e8a735; "
             + "font-weight: bold; font-size: 13px; border-top: 1px solid #333; "
             + "margin-top: 4px; }\n"
             + "        .access-groups .sub-conn:first-child { border-top: none; margin-top: 0; }\n"
             + "        .nmap-groups { padding: 12px 24px 20px; }\n"
             + "        .nmap-groups details { margin-bottom: 8px; border: 1px solid #e0e0e0; "
             + "border-radius: 6px; overflow: hidden; }\n"
             + "        .nmap-groups summary { padding: 10px 16px; cursor: pointer; "
             + "background: #f0f4f8; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; "
             + "font-size: 13px; font-weight: bold; color: #1a73e8; user-select: none; "
             + "list-style: none; display: flex; align-items: center; }\n"
             + "        .nmap-groups summary::-webkit-details-marker { display: none; }\n"
             + "        .nmap-groups summary::before { content: '▶'; margin-right: 8px; "
             + "font-size: 11px; transition: transform 0.2s; display: inline-block; }\n"
             + "        .nmap-groups details[open] summary::before { transform: rotate(90deg); }\n"
             + "        .nmap-groups summary:hover { background: #e3eaf5; }\n"
             + "        .nmap-groups .port-detail { background: #1e1e1e; color: #d4d4d4; "
             + "padding: 12px 16px; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; "
             + "font-size: 13px; line-height: 1.6; overflow-x: auto; white-space: pre-wrap; "
             + "word-wrap: break-word; }\n"
             + "        .nmap-groups .port-count { margin-left: auto; color: #888; "
             + "font-weight: normal; font-size: 12px; }\n"
             + "        .node-groups { padding: 12px 24px 20px; display: flex; "
             + "flex-wrap: wrap; gap: 10px; }\n"
             + "        .node-groups .node-item { background: #e8f0fe; color: #1a73e8; "
             + "padding: 6px 16px; border-radius: 16px; font-family: 'Consolas', 'Monaco', "
             + "'Courier New', monospace; font-size: 14px; font-weight: bold; "
             + "border: 1px solid #c5d9f2; }\n"
             + "        .node-groups .node-count { padding: 4px 0; width: 100%; "
             + "color: #666; font-size: 13px; }\n"
             + "        body.tampered { background: #fee !important; }\n"
             + "        body.tampered::before { content: '文档内容不完整'; "
             + "position: fixed; top: 0; left: 0; width: 100%; height: 100%; "
             + "display: flex; align-items: center; justify-content: center; "
             + "font-size: 80px; font-weight: bold; color: rgba(255,0,0,0.12); "
             + "pointer-events: none; z-index: 9999; "
             + "letter-spacing: 8px; white-space: nowrap; }\n"
             + "        body.tampered .header { background: linear-gradient(135deg, #d32f2f, #b71c1c) !important; }\n";
    }

    /**
     * 将节点IP集合构建为HTML标签列表。
     *
     * @param allIps 所有唯一IP地址集合
     * @return HTML片段
     */
    public String buildNodesHtml(Set<String> allIps) {
        if (allIps == null || allIps.isEmpty()) {
            return "            <p class=\"desc\" style=\"padding-bottom:16px;\">无数据</p>\n";
        }

        StringBuilder html = new StringBuilder();
        html.append("                <div class=\"node-count\">共发现 ")
            .append(allIps.size())
            .append(" 个节点</div>\n");
        for (String ip : allIps) {
            html.append("                <span class=\"node-item\">")
                .append(escapeHtml(ip))
                .append("</span>\n");
        }
        return html.toString();
    }

    /**
     * 将数据访问发现的合并结果构建为可折叠的HTML。
     * 输入格式（由 ResultProcessor.mergeDataAccessByConnection 生成）：
     * === 192.168.48.1 > 192.168.48.204 ===
     * >>> 192.168.48.1.50551 > 192.168.48.204.3306
     * SQL内容行1
     * >>> 192.168.48.1.50551 > 192.168.48.204.3306
     * SQL内容行2
     * --
     * <p>
     * 每个 IP > IP 分组生成一个 &lt;details&gt; 折叠块，折叠标题显示 IP > IP，
     * 展开后显示原始带端口的连接对子头行和带序号的SQL详情。
     *
     * @param dataAccessResult 合并后的数据访问结果文本
     * @return HTML片段
     */
    public String buildDataAccessHtml(String dataAccessResult) {
        if (dataAccessResult == null || dataAccessResult.trim().isEmpty()) {
            return "            <p class=\"desc\" style=\"padding-bottom:16px;\">无数据</p>\n";
        }

        StringBuilder html = new StringBuilder();
        String[] lines = dataAccessResult.split("\n");

        String currentConnection = null;
        java.util.List<String> currentSqls = new java.util.ArrayList<String>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 检查是否是 === connection > target === 头行
            if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                // 如果有上一组数据，先输出
                if (currentConnection != null) {
                    html.append(buildDetailsBlock(currentConnection, currentSqls));
                }
                // 提取连接对：去掉两端的 ===
                currentConnection = trimmed.replaceAll("^=+\\s*", "").replaceAll("\\s*=+$", "");
                currentSqls = new java.util.ArrayList<String>();
                continue;
            }

            // 遇到分隔符 "--"，输出当前组
            if ("--".equals(trimmed)) {
                if (currentConnection != null) {
                    html.append(buildDetailsBlock(currentConnection, currentSqls));
                    currentConnection = null;
                    currentSqls = new java.util.ArrayList<String>();
                }
                continue;
            }

            // 内容行
            currentSqls.add(trimmed);
        }

        // 处理最后一组（可能没有 "--" 结尾）
        if (currentConnection != null && !currentSqls.isEmpty()) {
            html.append(buildDetailsBlock(currentConnection, currentSqls));
        }

        return html.toString();
    }

    /**
     * 构建单个折叠块的HTML（nmap结果）
     *
     * @param ip    主机IP地址
     * @param lines 该IP下的所有输出行
     * @return details/summary HTML片段
     */
    private String buildNmapDetailsBlock(String ip, java.util.List<String> lines) {
        // 统计端口数：匹配 "XXXX/tcp  open  ..." 格式的行
        int portCount = 0;
        for (String line : lines) {
            if (line.matches("^\\d+/\\w+\\s+open\\s+.*")) {
                portCount++;
            }
        }

        StringBuilder block = new StringBuilder();
        block.append("            <details>\n");
        block.append("                <summary>")
             .append(escapeHtml(ip))
             .append("<span class=\"port-count\">（")
             .append(portCount)
             .append(" 个开放端口）</span></summary>\n");
        block.append("                <div class=\"port-detail\">");

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                block.append("\n");
            }
            block.append(escapeHtml(lines.get(i)));
        }

        block.append("</div>\n");
        block.append("            </details>\n");
        return block.toString();
    }

    /**
     * 将 nmap 的分组合并结果构建为可折叠的HTML。
     * 输入格式（由 ResultProcessor.mergeNmapByHost 生成）：
     * === 127.0.0.1 ===
     * Discovered open port 8080/tcp on 127.0.0.1
     * 8080/tcp  open   http-proxy
     * --
     * <p>
     * 每个 IP 生成一个 &lt;details&gt; 折叠块，折叠标题显示 IP，
     * 展开后显示该IP下的端口发现详情。
     *
     * @param nmapResult 合并后的 nmap 结果文本
     * @return HTML片段
     */
    public String buildNmapHtml(String nmapResult) {
        if (nmapResult == null || nmapResult.trim().isEmpty()) {
            return "            <p class=\"desc\" style=\"padding-bottom:16px;\">无数据</p>\n";
        }

        StringBuilder html = new StringBuilder();
        String[] lines = nmapResult.split("\n");

        String currentIp = null;
        java.util.List<String> currentLines = new java.util.ArrayList<String>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 检查是否是 === IP === 头行
            if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                // 如果有上一组数据，先输出
                if (currentIp != null) {
                    html.append(buildNmapDetailsBlock(currentIp, currentLines));
                }
                // 提取IP：去掉两端的 ===
                currentIp = trimmed.replaceAll("^=+\\s*", "").replaceAll("\\s*=+$", "");
                currentLines = new java.util.ArrayList<String>();
                continue;
            }

            // 遇到分隔符 "--"，输出当前组
            if ("--".equals(trimmed)) {
                if (currentIp != null) {
                    html.append(buildNmapDetailsBlock(currentIp, currentLines));
                    currentIp = null;
                    currentLines = new java.util.ArrayList<String>();
                }
                continue;
            }

            // 内容行
            currentLines.add(trimmed);
        }

        // 处理最后一组（可能没有 "--" 结尾）
        if (currentIp != null && !currentLines.isEmpty()) {
            html.append(buildNmapDetailsBlock(currentIp, currentLines));
        }

        return html.toString();
    }

    /**
     * 构建单个折叠块的HTML
     *
     * @param connection 连接对字符串，如 "192.168.48.1 > 192.168.48.204"（IP > IP 无端口）
     * @param sqlLines   该连接对下的所有内容行列表（包含 ">>> IP.PORT > IP.PORT" 子头行和SQL内容行）
     * @return details/summary HTML片段
     */
    private String buildDetailsBlock(String connection, java.util.List<String> sqlLines) {
        // 统计真正的SQL条数（不含 >>> 子头行）
        int sqlCount = 0;
        for (String line : sqlLines) {
            if (!line.startsWith(">>> ")) {
                sqlCount++;
            }
        }

        StringBuilder block = new StringBuilder();
        block.append("            <details>\n");
        block.append("                <summary>")
             .append(escapeHtml(connection))
             .append("<span class=\"sql-count\">（")
             .append(sqlCount)
             .append(" 条SQL）</span></summary>\n");
        block.append("                <div class=\"sql-detail\">\n");

        // 带序号输出每条SQL，子头行特殊样式
        int sqlNo = 0;
        for (int i = 0; i < sqlLines.size(); i++) {
            String line = sqlLines.get(i);
            if (line.startsWith(">>> ")) {
                // 子连接头行：显示原始带端口的连接对
                String subConn = line.substring(4);
                block.append("                    <div class=\"sub-conn\">")
                     .append(escapeHtml(subConn))
                     .append("</div>\n");
            } else {
                // SQL内容行：带序号
                sqlNo++;
                block.append("                    <div class=\"sql-line\">")
                     .append("<span class=\"sql-no\">")
                     .append(sqlNo)
                     .append(".</span>")
                     .append("<span class=\"sql-text\">")
                     .append(escapeHtml(line))
                     .append("</span></div>\n");
            }
        }

        block.append("                </div>\n");
        block.append("            </details>\n");
        return block.toString();
    }

    /**
     * 将内容字符串规范化为与浏览器 innerHTML 一致的形式。
     * <p>
     * 浏览器在读取 innerHTML 时会进行以下转换：
     * - 文本内容中的 &amp;quot; 还原为 "
     * - 文本内容中的 &amp;#39; 还原为 '
     * - &amp;amp;、&amp;lt;、&amp;gt; 保持不变
     *
     * @param content 原始HTML内容
     * @return 规范化后的字符串
     */
    public static String normalizeForIntegrity(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("&quot;", "\"").replace("&#39;", "'");
    }

    /**
     * 计算字符串的 SHA-256 哈希值，返回十六进制字符串。
     *
     * @param content 要计算哈希的内容
     * @return SHA-256 哈希的十六进制表示
     */
    public static String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 生成完整性校验的 JavaScript 脚本。
     * 页面加载后，JS 会重新计算 #report-content 内容的 SHA-256 哈希，
     * 与 data-integrity 属性中存储的原始哈希对比。
     * 若不一致，则设置 body 为红色背景并显示“文档内容不完整”水印。
     *
     * @return JavaScript 脚本字符串
     */
    private String getIntegrityScript() {
        return "    <script>\n"
             + "    (function(){\n"
             + "        function sha256(str){\n"
             + "            function rr(n,x){return(x>>>n)|(x<<(32-n));}\n"
             + "            var K=[0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,"
             + "0x923f82a4,0xab1c5ed5,0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,"
             + "0x80deb1fe,0x9bdc06a7,0xc19bf174,0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,"
             + "0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,0x983e5152,0xa831c66d,0xb00327c8,"
             + "0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,0x27b70a85,0x2e1b2138,"
             + "0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,0xa2bfe8a1,"
             + "0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,"
             + "0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,"
             + "0x682e6ff3,0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,"
             + "0xbef9a3f7,0xc67178f2];\n"
             + "            var H=[0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,"
             + "0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19];\n"
             + "            var enc=new TextEncoder(),d=enc.encode(str);\n"
             + "            var l=d.length,bl=l*8;\n"
             + "            var pLen=((l+9+63)&~63);\n"
             + "            var buf=new Uint8Array(pLen);\n"
             + "            buf.set(d);buf[l]=0x80;\n"
             + "            var dv=new DataView(buf.buffer);\n"
             + "            dv.setUint32(pLen-4,bl,false);\n"
             + "            for(var off=0;off<pLen;off+=64){\n"
             + "                var w=new Array(64);\n"
             + "                for(var i=0;i<16;i++) w[i]=dv.getUint32(off+i*4,false);\n"
             + "                for(var i=16;i<64;i++){\n"
             + "                    var s0=(rr(7,w[i-15])^rr(18,w[i-15])^(w[i-15]>>>3));\n"
             + "                    var s1=(rr(17,w[i-2])^rr(19,w[i-2])^(w[i-2]>>>10));\n"
             + "                    w[i]=(w[i-16]+s0+w[i-7]+s1)|0;\n"
             + "                }\n"
             + "                var a=H[0],b=H[1],c=H[2],dd=H[3],e=H[4],f=H[5],g=H[6],h=H[7];\n"
             + "                for(var i=0;i<64;i++){\n"
             + "                    var S1=(rr(6,e)^rr(11,e)^rr(25,e));\n"
             + "                    var ch=((e&f)^((~e)&g));\n"
             + "                    var t1=(h+S1+ch+K[i]+w[i])|0;\n"
             + "                    var S0=(rr(2,a)^rr(13,a)^rr(22,a));\n"
             + "                    var mj=((a&b)^(a&c)^(b&c));\n"
             + "                    var t2=(S0+mj)|0;\n"
             + "                    h=g;g=f;f=e;e=(dd+t1)|0;dd=c;c=b;b=a;a=(t1+t2)|0;\n"
             + "                }\n"
             + "                H[0]=(H[0]+a)|0;H[1]=(H[1]+b)|0;H[2]=(H[2]+c)|0;H[3]=(H[3]+dd)|0;\n"
             + "                H[4]=(H[4]+e)|0;H[5]=(H[5]+f)|0;H[6]=(H[6]+g)|0;H[7]=(H[7]+h)|0;\n"
             + "            }\n"
             + "            var hex='';\n"
             + "            for(var i=0;i<8;i++){\n"
             + "                for(var j=24;j>=0;j-=8){\n"
             + "                    var b2=(H[i]>>>j)&0xff;\n"
             + "                    hex+=(b2<16?'0':'')+b2.toString(16);\n"
             + "                }\n"
             + "            }\n"
             + "            return hex;\n"
             + "        }\n"
             + "        var el=document.getElementById('report-content');\n"
             + "        var expected=el.getAttribute('data-integrity');\n"
             + "        var actual=sha256(el.innerHTML);\n"
             + "        if(actual!==expected){\n"
             + "            document.body.classList.add('tampered');\n"
             + "        }\n"
             + "    })();\n"
             + "    </script>\n";
    }

    /**
     * HTML特殊字符转义，防止XSS
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
