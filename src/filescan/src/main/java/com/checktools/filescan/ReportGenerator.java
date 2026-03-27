package com.checktools.filescan;

import com.checktools.filescan.model.AnalysisResult;
import com.checktools.filescan.model.SensitiveData;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * HTML报告生成器
 * 负责将分析结果生成HTML格式的报告
 * 包含防篡改功能：使用SHA-256对报告内容计算哈希值，
 * 在HTML中嵌入JS校验逻辑，篡改后背景变红并显示"内容不完整"
 */
public class ReportGenerator {

    /**
     * 生成HTML报告并保存到文件
     *
     * @param result     分析结果
     * @param outputPath 输出的HTML文件路径
     * @throws IOException 文件写入异常
     */
    public void generateReport(AnalysisResult result, String outputPath) throws IOException {
        String htmlContent = buildHtmlContent(result);

        // 写入文件
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outputPath), "UTF-8")) {
            writer.write(htmlContent);
        }
    }

    /**
     * 生成HTML报告内容字符串
     *
     * @param result 分析结果
     * @return HTML内容
     */
    public String buildHtmlContent(AnalysisResult result) {
        // 先构建报告主体内容（用于计算哈希）
        String bodyContent = buildBodyContent(result);

        // 提取纯文本内容并归一化空白，用于防篡改哈希校验
        // 这样Java端和JS端（textContent）得到的文本完全一致
        String textForHash = stripHtmlForHash(bodyContent);
        String contentHash = calculateHash(textForHash);

        // 构建完整的HTML页面
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>文件扫描分析报告</title>\n");
        html.append(buildStyleSection());
        html.append("</head>\n");
        html.append("<body>\n");

        // 防篡改提示区域（默认隐藏）
        html.append("    <div id=\"tamper-warning\" class=\"tamper-warning\" style=\"display:none;\">\n");
        html.append("        内容不完整\n");
        html.append("    </div>\n");

        // 报告主体内容区域
        html.append("    <div id=\"report-content\">\n");
        html.append(bodyContent);
        html.append("    </div>\n");

        // 嵌入哈希值（隐藏）
        html.append("    <div id=\"content-hash\" style=\"display:none;\">");
        html.append(contentHash);
        html.append("</div>\n");

        // 防篡改校验脚本
        html.append(buildScriptSection());

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    /**
     * 构建报告主体HTML内容
     *
     * @param result 分析结果
     * @return 主体内容HTML
     */
    String buildBodyContent(AnalysisResult result) {
        StringBuilder body = new StringBuilder();

        // 标题
        body.append("        <h1>文件扫描分析报告</h1>\n");

        // 基本信息区域
        body.append("        <div class=\"section\">\n");
        body.append("            <h2>基本信息</h2>\n");
        body.append("            <table>\n");
        body.append("                <tr><td class=\"label\">文件路径</td><td>");
        body.append(escapeHtml(result.getFilePath()));
        body.append("</td></tr>\n");
        body.append("                <tr><td class=\"label\">文件大小</td><td>");
        body.append(formatFileSize(result.getFileSize()));
        body.append("</td></tr>\n");
        body.append("                <tr><td class=\"label\">可见字符数</td><td>");
        body.append(result.getVisibleCharCount());
        if (result.isReachedLimit()) {
            body.append(" (已达上限1000000)");
        }
        body.append("</td></tr>\n");
        body.append("                <tr><td class=\"label\">分析时间</td><td>");
        body.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        body.append("</td></tr>\n");
        body.append("            </table>\n");
        body.append("        </div>\n");

        // 敏感数据区域
        body.append("        <div class=\"section\">\n");
        body.append("            <h2>敏感数据检测结果</h2>\n");
        List<SensitiveData> sensitiveList = result.getSensitiveDataList();
        if (sensitiveList.isEmpty()) {
            body.append("            <p class=\"no-data\">未检测到敏感数据</p>\n");
        } else {
            body.append("            <table>\n");
            body.append("                <tr><th>序号</th><th>类型</th><th>内容</th><th>位置</th></tr>\n");
            for (int i = 0; i < sensitiveList.size(); i++) {
                SensitiveData data = sensitiveList.get(i);
                body.append("                <tr>");
                body.append("<td>").append(i + 1).append("</td>");
                body.append("<td>").append(escapeHtml(data.getType().getDisplayName())).append("</td>");
                body.append("<td>").append(maskSensitiveContent(data)).append("</td>");
                body.append("<td>").append(data.getStartIndex()).append("</td>");
                body.append("</tr>\n");
            }
            body.append("            </table>\n");
        }
        body.append("        </div>\n");

        // 可见字符展示区域
        body.append("        <div class=\"section\">\n");
        body.append("            <h2>可见字符内容</h2>\n");
        body.append("            <div class=\"visible-chars\">");
        String visibleChars = result.getVisibleChars();
        if (visibleChars != null && !visibleChars.isEmpty()) {
            body.append(escapeHtml(visibleChars));
        } else {
            body.append("未提取到可见字符");
        }
        body.append("</div>\n");
        body.append("        </div>\n");

        return body.toString();
    }

    /**
     * 构建CSS样式区域
     */
    private String buildStyleSection() {
        StringBuilder style = new StringBuilder();
        style.append("    <style>\n");
        style.append("        body {\n");
        style.append("            font-family: 'Microsoft YaHei', Arial, sans-serif;\n");
        style.append("            margin: 20px;\n");
        style.append("            background-color: #f5f5f5;\n");
        style.append("            transition: background-color 0.3s;\n");
        style.append("        }\n");
        style.append("        body.tampered {\n");
        style.append("            background-color: #ff0000 !important;\n");
        style.append("        }\n");
        style.append("        .tamper-warning {\n");
        style.append("            text-align: center;\n");
        style.append("            font-size: 36px;\n");
        style.append("            font-weight: bold;\n");
        style.append("            color: white;\n");
        style.append("            padding: 30px;\n");
        style.append("            margin-bottom: 20px;\n");
        style.append("        }\n");
        style.append("        h1 {\n");
        style.append("            text-align: center;\n");
        style.append("            color: #333;\n");
        style.append("            border-bottom: 2px solid #4CAF50;\n");
        style.append("            padding-bottom: 10px;\n");
        style.append("        }\n");
        style.append("        .section {\n");
        style.append("            background-color: white;\n");
        style.append("            border-radius: 8px;\n");
        style.append("            padding: 20px;\n");
        style.append("            margin: 15px 0;\n");
        style.append("            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n");
        style.append("        }\n");
        style.append("        h2 {\n");
        style.append("            color: #4CAF50;\n");
        style.append("            border-left: 4px solid #4CAF50;\n");
        style.append("            padding-left: 10px;\n");
        style.append("        }\n");
        style.append("        table {\n");
        style.append("            width: 100%;\n");
        style.append("            border-collapse: collapse;\n");
        style.append("            margin-top: 10px;\n");
        style.append("        }\n");
        style.append("        th, td {\n");
        style.append("            border: 1px solid #ddd;\n");
        style.append("            padding: 10px;\n");
        style.append("            text-align: left;\n");
        style.append("        }\n");
        style.append("        th {\n");
        style.append("            background-color: #4CAF50;\n");
        style.append("            color: white;\n");
        style.append("        }\n");
        style.append("        tr:nth-child(even) {\n");
        style.append("            background-color: #f9f9f9;\n");
        style.append("        }\n");
        style.append("        .label {\n");
        style.append("            font-weight: bold;\n");
        style.append("            width: 120px;\n");
        style.append("            background-color: #f0f0f0;\n");
        style.append("        }\n");
        style.append("        .visible-chars {\n");
        style.append("            background-color: #f9f9f9;\n");
        style.append("            border: 1px solid #ddd;\n");
        style.append("            padding: 15px;\n");
        style.append("            word-wrap: break-word;\n");
        style.append("            white-space: pre-wrap;\n");
        style.append("            max-height: 400px;\n");
        style.append("            overflow-y: auto;\n");
        style.append("            font-family: 'Courier New', monospace;\n");
        style.append("            font-size: 13px;\n");
        style.append("            line-height: 1.6;\n");
        style.append("        }\n");
        style.append("        .no-data {\n");
        style.append("            color: #999;\n");
        style.append("            font-style: italic;\n");
        style.append("        }\n");
        style.append("    </style>\n");
        return style.toString();
    }

    /**
     * 构建防篡改校验的JavaScript脚本
     * 使用Web Crypto API计算报告内容的SHA-256哈希值并与嵌入的哈希值进行比对
     */
    private String buildScriptSection() {
        StringBuilder script = new StringBuilder();
        script.append("    <script>\n");
        script.append("    (function() {\n");
        script.append("        // 防篡改校验：页面加载后计算报告内容的textContent哈希并与预存哈希对比\n");
        script.append("        window.addEventListener('load', function() {\n");
        script.append("            // 使用textContent获取纯文本，并归一化空白字符，与Java端保持一致\n");
        script.append("            var content = document.getElementById('report-content').textContent.replace(/\\s+/g, ' ').trim();\n");
        script.append("            var storedHash = document.getElementById('content-hash').textContent.trim();\n");
        script.append("            \n");
        script.append("            // 使用Web Crypto API计算SHA-256\n");
        script.append("            if (window.crypto && window.crypto.subtle) {\n");
        script.append("                var encoder = new TextEncoder();\n");
        script.append("                var data = encoder.encode(content);\n");
        script.append("                window.crypto.subtle.digest('SHA-256', data).then(function(hashBuffer) {\n");
        script.append("                    var hashArray = Array.from(new Uint8Array(hashBuffer));\n");
        script.append("                    var hashHex = hashArray.map(function(b) {\n");
        script.append("                        return b.toString(16).padStart(2, '0');\n");
        script.append("                    }).join('');\n");
        script.append("                    \n");
        script.append("                    if (hashHex !== storedHash) {\n");
        script.append("                        // 哈希不匹配，报告被篡改\n");
        script.append("                        document.body.classList.add('tampered');\n");
        script.append("                        document.getElementById('tamper-warning').style.display = 'block';\n");
        script.append("                    }\n");
        script.append("                });\n");
        script.append("            } else {\n");
        script.append("                console.warn('浏览器不支持Web Crypto API，无法进行防篡改校验');\n");
        script.append("            }\n");
        script.append("        });\n");
        script.append("    })();\n");
        script.append("    </script>\n");
        return script.toString();
    }

    /**
     * 从HTML中提取纯文本内容，解码HTML实体并归一化空白
     * 这与浏览器中 textContent.replace(/\s+/g, ' ').trim() 的结果一致
     *
     * @param html HTML内容
     * @return 归一化后的纯文本
     */
    static String stripHtmlForHash(String html) {
        // 去除HTML标签
        String text = html.replaceAll("<[^>]*>", "");
        // 解码HTML实体（与浏览器textContent自动解码保持一致）
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'");
        // 归一化空白字符（与JS端的replace(/\s+/g, ' ').trim()一致）
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    /**
     * 计算文本的SHA-256哈希值
     *
     * @param content 要计算哈希的文本
     * @return 哈希值的十六进制字符串
     */
    static String calculateHash(String content) {
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
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * HTML特殊字符转义
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 对敏感数据内容进行脱敏处理
     * 身份证号：显示前6位和最后4位，中间用*替代
     * 手机号：显示前3位和最后4位，中间用****替代
     * 邮箱：显示前2个字符和@后的域名，中间用***替代
     *
     * @param data 敏感数据
     * @return 脱敏后的内容
     */
    static String maskSensitiveContent(SensitiveData data) {
        String content = data.getContent();
        switch (data.getType()) {
            case ID_CARD:
                if (content.length() >= 10) {
                    return content.substring(0, 6) + "********" + content.substring(content.length() - 4);
                }
                break;
            case PHONE:
                if (content.length() == 11) {
                    return content.substring(0, 3) + "****" + content.substring(7);
                }
                break;
            case EMAIL:
                int atIndex = content.indexOf('@');
                if (atIndex > 2) {
                    return content.substring(0, 2) + "***" + content.substring(atIndex);
                }
                break;
        }
        return content;
    }

    /**
     * 格式化文件大小为人类可读的格式
     *
     * @param size 文件大小（字节）
     * @return 格式化后的字符串
     */
    static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
