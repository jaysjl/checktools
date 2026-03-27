package com.dbscan.scan;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 报告生成器，输出带校验和的 HTML 格式扫描报告。
 * 校验原理：Java 侧和浏览器侧使用同一套逻辑从结构化数据构建纯文本载荷，
 * 再对该载荷做 SHA-256 哈希比对，避免浏览器 HTML 规范化导致的误判。
 */
public class ReportGenerator {
    private static final Charset OUTPUT_CHARSET = StandardCharsets.UTF_8;
    private static final DateTimeFormatter REPORT_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成 HTML 格式的报告
     * @param results 扫描结果列表
     * @param outputPath 输出文件路径
     * @throws IOException 文件写入异常
     */
    public static void generateReport(List<ScanResult> results, String outputPath) throws IOException {
        Path reportPath = Paths.get(outputPath);
        if (reportPath.getParent() != null) {
            Files.createDirectories(reportPath.getParent());
        }

        String generatedAt = REPORT_TIME_FMT.format(LocalDateTime.now());
        String checksumPayload = buildChecksumPayload(results, generatedAt);
        String checksum = sha256(checksumPayload);
        String html = buildHtml(results, generatedAt, checksum);

        Files.write(reportPath, html.getBytes(OUTPUT_CHARSET));
        System.out.println("报告已生成: " + outputPath);
    }

    /**
     * 构建用于校验的纯文本载荷（Java 侧与浏览器侧完全一致）
     */
    static String buildChecksumPayload(List<ScanResult> results, String generatedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("GENERATED_AT=").append(generatedAt).append('\n');
        sb.append("HIT_COUNT=").append(results.size()).append('\n');

        for (ScanResult r : results) {
            sb.append("ROW=");
            sb.append(safe(r.getIp())).append('\u001F');
            sb.append(safe(String.valueOf(r.getPort()))).append('\u001F');
            sb.append(safe(r.getDatabaseType())).append('\u001F');
            sb.append(safe(r.getDatabase())).append('\u001F');
            sb.append(safe(r.getSchema())).append('\u001F');
            sb.append(safe(r.getTable())).append('\u001F');
            sb.append(safe(r.getColumn())).append('\u001F');
            sb.append(safe(String.valueOf(r.getExtractCount()))).append('\u001F');
            sb.append(safe(r.getRuleName())).append('\u001F');
            sb.append(safe(r.getRuleDescription())).append('\u001F');
            sb.append(fmtRate(r.getMatchRate())).append('\u001F');
            sb.append(safe(r.getSamples()));
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String buildHtml(List<ScanResult> results, String generatedAt, String checksum) {
        StringBuilder tb = new StringBuilder();
        if (results.isEmpty()) {
            tb.append("    <tr><td colspan=\"12\" class=\"empty\">未发现敏感数据命中</td></tr>\n");
        } else {
            for (ScanResult r : results) {
                tb.append("    <tr data-row>");
                tb.append(td(r.getIp()));
                tb.append(td(String.valueOf(r.getPort())));
                tb.append(td(r.getDatabaseType()));
                tb.append(td(r.getDatabase()));
                tb.append(td(r.getSchema()));
                tb.append(td(r.getTable()));
                tb.append(td(r.getColumn()));
                tb.append(td(String.valueOf(r.getExtractCount())));
                tb.append(td(r.getRuleName()));
                tb.append(td(r.getRuleDescription()));
                tb.append(td(fmtRate(r.getMatchRate())));
                tb.append(td(r.getSamples()));
                tb.append("</tr>\n");
            }
        }

        return "<!DOCTYPE html>\n"
            + "<html lang=\"zh-CN\">\n"
            + "<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n"
            + "<title>dbscan 扫描报告</title>\n"
            + "<style>\n"
            + "body{margin:0;padding:24px;font-family:Arial,sans-serif;background:#f5f7fb;color:#1f2937}\n"
            + "body.tampered{background:#fee2e2}\n"
            + ".wrap{max-width:1400px;margin:0 auto}\n"
            + "h1{margin:0 0 4px;font-size:26px}\n"
            + ".sub{color:#6b7280;margin:0 0 16px}\n"
            + ".banner{display:none;padding:14px 18px;border-radius:10px;font-weight:bold;margin-bottom:18px}\n"
            + ".banner.bad{display:block;background:#fee2e2;color:#b91c1c;border:1px solid #fca5a5}\n"
            + ".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:14px;margin-bottom:18px}\n"
            + ".card{background:#fff;border-radius:10px;padding:14px;box-shadow:0 2px 12px rgba(0,0,0,.05)}\n"
            + ".card .lbl{display:block;color:#6b7280;font-size:13px;margin-bottom:6px}\n"
            + "table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.05)}\n"
            + "th,td{padding:10px 12px;border-bottom:1px solid #e5e7eb;text-align:left;vertical-align:top}\n"
            + "th{background:#111827;color:#fff;position:sticky;top:0}\n"
            + "tr:nth-child(even) td{background:#f9fafb}\n"
            + ".empty{text-align:center;color:#6b7280;padding:28px}\n"
            + "</style>\n"
            + "</head>\n"
            + "<body>\n"
            + "<div class=\"wrap\">\n"
            + "  <h1>dbscan 扫描报告</h1>\n"
            + "  <p class=\"sub\">敏感数据扫描结果</p>\n"
            + "  <div id=\"banner\" class=\"banner\"></div>\n"
            + "  <div class=\"cards\">\n"
            + "    <div class=\"card\"><span class=\"lbl\">命中总数</span><strong id=\"hit-count\">" + results.size() + "</strong></div>\n"
            + "    <div class=\"card\"><span class=\"lbl\">生成时间</span><strong id=\"gen-time\">" + esc(generatedAt) + "</strong></div>\n"
            + "  </div>\n"
            + "  <table>\n"
            + "  <thead><tr>"
            + "<th>IP</th><th>端口</th><th>数据库类型</th><th>库名</th><th>模式名</th>"
            + "<th>表名</th><th>列名</th><th>抽取总数</th><th>规则名称</th><th>规则描述</th>"
            + "<th>匹配率</th><th>采样</th>"
            + "</tr></thead>\n"
            + "  <tbody id=\"data-body\">\n"
            + tb.toString()
            + "  </tbody>\n"
            + "  </table>\n"
            + ""
            + "</div>\n"
            + "<script>\n"
            + "(function(){\n"
            + "var banner=document.getElementById('banner');\n"
            + "var expected='" + checksum + "';\n"
            + "\n"
            + "function s(v){return v==null?'':String(v)}\n"
            + "\n"
            + "/* 与 Java 侧 buildChecksumPayload 完全一致的逻辑 */\n"
            + "function buildPayload(){\n"
            + "  var lines=[];\n"
            + "  lines.push('GENERATED_AT='+s(document.getElementById('gen-time').textContent));\n"
            + "  lines.push('HIT_COUNT='+s(document.getElementById('hit-count').textContent));\n"
            + "  var rows=document.querySelectorAll('#data-body tr[data-row]');\n"
            + "  for(var i=0;i<rows.length;i++){\n"
            + "    var cells=rows[i].querySelectorAll('td');\n"
            + "    var vals=[];\n"
            + "    for(var j=0;j<cells.length;j++) vals.push(s(cells[j].textContent));\n"
            + "    lines.push('ROW='+vals.join('\\u001F'));\n"
            + "  }\n"
            + "  return lines.join('\\n')+'\\n';\n"
            + "}\n"
            + "\n"
            + "function hex(buf){\n"
            + "  return Array.prototype.map.call(new Uint8Array(buf),function(b){\n"
            + "    return('0'+b.toString(16)).slice(-2);\n"
            + "  }).join('');\n"
            + "}\n"
            + "\n"
            + "if(!window.crypto||!window.crypto.subtle){\n"
            + "  document.body.classList.add('tampered');\n"
            + "  banner.className='banner bad';banner.textContent='浏览器不支持校验，无法确认报告完整性。';\n"
            + "  return;\n"
            + "}\n"
            + "\n"
            + "var payload=buildPayload();\n"
            + "crypto.subtle.digest('SHA-256',new TextEncoder().encode(payload)).then(function(buf){\n"
            + "  var actual=hex(buf);\n"
            + "  if(actual!==expected){\n"
            + "    document.body.classList.add('tampered');\n"
            + "    banner.className='banner bad';\n"
            + "    banner.textContent='内容不完整';\n"
            + "  }\n"
            + "});\n"
            + "})();\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";
    }

    private static String td(String v) { return "<td>" + esc(v) + "</td>"; }
    private static String fmtRate(Double r) { return String.format("%.2f%%", r == null ? 0.0 : r); }
    private static String safe(String v) { return v == null ? "" : v; }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(OUTPUT_CHARSET));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 输出扫描结果摘要
     * @param results 扫描结果列表
     */
    public static void printSummary(List<ScanResult> results) {
        System.out.println("\n========== 扫描完成 ==========");
        System.out.println("发现敏感数据命中数: " + results.size());

        if (!results.isEmpty()) {
            System.out.println("\n命中详情：");
            results.forEach(result -> {
                long matchCount = Math.round(result.getMatchRate() * result.getExtractCount() / 100.0);
                System.out.println(String.format("  %s.%s.%s: %d 条 (规则: %s)",
                        result.getSchema(), result.getTable(), result.getColumn(),
                        matchCount, result.getRuleName()));
            });
        }
        System.out.println("==============================\n");
    }
}
