package com.checktools.filescan;

import com.checktools.filescan.model.AnalysisResult;

import java.io.*;

/**
 * 文件分析器
 * 负责调用 personal_data_discovery_from_file.sh 脚本提取文件中的可见字符（中文、英文、数字等）
 * 最多提取前10000个可见字符，达到上限后停止
 */
public class FileAnalyzer {

    /** 可见字符最大提取数量 */
    static final int MAX_VISIBLE_CHARS = 1000000;

    /** 脚本文件名 */
    private static final String SCRIPT_NAME = "personal_data_discovery_from_file.sh";

    /** 脚本路径 */
    private String scriptPath;

    /**
     * 默认构造方法，自动查找脚本路径
     */
    public FileAnalyzer() {
        this.scriptPath = findScriptPath();
    }

    /**
     * 指定脚本路径的构造方法
     *
     * @param scriptPath 脚本文件的完整路径
     */
    public FileAnalyzer(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    /**
     * 分析指定文件，提取可见字符并检测敏感数据
     *
     * @param filePath 要分析的文件路径
     * @return 分析结果
     * @throws IOException 文件读取异常
     */
    public AnalysisResult analyze(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        if (!file.isFile()) {
            throw new IOException("路径不是文件: " + filePath);
        }

        AnalysisResult result = new AnalysisResult(filePath);
        result.setFileSize(file.length());

        // 调用脚本从文件中提取可见字符
        String visibleChars = extractVisibleCharsByScript(filePath);

        // 截断到最大字符数限制
        if (visibleChars.length() > MAX_VISIBLE_CHARS) {
            visibleChars = visibleChars.substring(0, MAX_VISIBLE_CHARS);
            result.setReachedLimit(true);
        } else {
            result.setReachedLimit(false);
        }

        result.setVisibleChars(visibleChars);
        result.setVisibleCharCount(visibleChars.length());

        // 使用敏感数据检测器分析可见字符中的敏感信息
        SensitiveDataDetector detector = new SensitiveDataDetector();
        result.setSensitiveDataList(detector.detect(visibleChars));

        return result;
    }

    /**
     * 调用 personal_data_discovery_from_file.sh 脚本提取文件中的可见字符
     * 脚本使用 perl/sed/awk/grep 管道处理二进制文件，提取可打印字符串
     *
     * @param filePath 要分析的文件路径
     * @return 提取到的可见字符串
     * @throws IOException 脚本执行异常
     */
    private String extractVisibleCharsByScript(String filePath) throws IOException {
        if (scriptPath == null || !new File(scriptPath).exists()) {
            throw new IOException("脚本文件不存在: " + scriptPath
                    + "，请确保 scripts/" + SCRIPT_NAME + " 存在");
        }

        // 构建执行命令：bash script.sh -P all <filePath>
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, "-P", "all", filePath);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        // 读取脚本标准输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append("\n");
                }
                output.append(line);

                // 已超过上限，提前停止读取以节省资源
                if (output.length() >= MAX_VISIBLE_CHARS) {
                    break;
                }
            }
        }

        // 等待进程结束
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("脚本执行被中断", e);
        } finally {
            process.destroy();
        }

        return output.toString();
    }

    /**
     * 查找脚本路径
     * 优先使用系统属性 filescan.script.dir，否则在当前工作目录下的 scripts 目录查找
     *
     * @return 脚本路径，找不到返回null
     */
    private String findScriptPath() {
        // 尝试获取jar包所在目录
        try {
            String jarPath = FileAnalyzer.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                // 运行的是jar包，返回jar包所在目录
                String jarDir = jarFile.getParent();
                File script = new File(jarDir + "/scripts", SCRIPT_NAME);
                if (script.exists()) {
                    return script.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            // 忽略异常，使用当前目录
        }

        // 在当前工作目录下的 scripts 目录查找
        File script = new File("scripts", SCRIPT_NAME);
        if (script.exists()) {
            return script.getAbsolutePath();
        }

        // 优先使用系统属性指定的脚本目录
        String scriptDir = System.getProperty("filescan.script.dir");
        if (scriptDir != null) {
            script = new File(scriptDir, SCRIPT_NAME);
            if (script.exists()) {
                return script.getAbsolutePath();
            }
        }

        return null;
    }

    // ========== 工具方法（保留用于其他场景） ==========

    /**
     * 判断ASCII字符是否为可见字符
     * 包括：英文字母、数字、常见标点和符号
     *
     * @param c 字符
     * @return 是否为可见字符
     */
    static boolean isVisibleAscii(char c) {
        return c >= '!' && c <= '~';
    }

    /**
     * 判断一个字符是否为可见字符
     * 包括：中文字符、英文字母、数字、常见标点
     *
     * @param c 字符
     * @return 是否为可见字符
     */
    static boolean isVisibleChar(char c) {
        if (isVisibleAscii(c)) {
            return true;
        }
        if (c >= '\u4E00' && c <= '\u9FFF') {
            return true;
        }
        if (c >= '\u3000' && c <= '\u303F') {
            return true;
        }
        if (c >= '\uFF00' && c <= '\uFFEF') {
            return true;
        }
        return false;
    }
}
