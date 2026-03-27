package com.netscan;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * 脚本执行器
 * 负责调用外部shell脚本，并提供实时输出和进度显示功能
 */
public class ScriptExecutor {

    /**
     * 行检查回调接口
     * 用于在脚本执行过程中判断是否应该提前停止
     */
    public interface LineChecker {
        /**
         * 检查当前累积的输出是否满足提前停止条件
         *
         * @param currentOutput 当前累积的全部输出
         * @return true 表示应提前停止
         */
        boolean shouldStop(String currentOutput);
    }

    /** 脚本所在目录 */
    private final String scriptsDir;

    /**
     * 构造脚本执行器
     *
     * @param scriptsDir 脚本所在目录的绝对路径
     */
    public ScriptExecutor(String scriptsDir) {
        this.scriptsDir = scriptsDir;
    }

    /**
     * 执行脚本并实时输出到控制台（用于nmap扫描）
     * 脚本的stdout会实时打印到控制台，让用户知道程序在工作
     *
     * @param scriptName 脚本文件名
     * @return 脚本执行的全部输出
     * @throws IOException          脚本文件不存在或执行失败
     * @throws InterruptedException 等待脚本执行被中断
     */
    public String executeWithRealtimeOutput(String scriptName) throws IOException, InterruptedException {
        String scriptPath = scriptsDir + File.separator + scriptName;
        validateScript(scriptPath);

        // 构建进程：使用bash执行脚本
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath);
        pb.directory(new File(scriptsDir));
        pb.redirectErrorStream(true); // 合并stderr到stdout

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        // 实时读取脚本输出并打印到控制台
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("  | " + line);
                output.append(line).append("\n");
            }
        }

        process.waitFor();
        return output.toString();
    }

    /**
     * 执行脚本指定时长，并在控制台显示进度条（用于tcpdump扫描）
     * 脚本运行指定秒数后自动终止，期间在控制台显示进度
     *
     * @param scriptName      脚本文件名
     * @param durationSeconds 执行时长（秒）
     * @return 脚本执行的全部输出
     * @throws IOException          脚本文件不存在或执行失败
     * @throws InterruptedException 等待脚本执行被中断
     */
    public String executeWithProgress(String scriptName, int durationSeconds)
            throws IOException, InterruptedException {
        return executeWithProgress(scriptName, durationSeconds, null);
    }

    /**
     * 执行脚本指定时长，并在控制台显示进度条，支持通过 LineChecker 提前停止。
     * 每次读到新输出后回调 checker，当 checker 返回 true 时提前终止脚本。
     *
     * @param scriptName      脚本文件名
     * @param durationSeconds 执行时长（秒）
     * @param checker         行检查器，返回 true 表示应该提前停止；null 表示不检查
     * @return 脚本执行的全部输出
     * @throws IOException          脚本文件不存在或执行失败
     * @throws InterruptedException 等待脚本执行被中断
     */
    public String executeWithProgress(String scriptName, int durationSeconds, final LineChecker checker)
            throws IOException, InterruptedException {
        String scriptPath = scriptsDir + File.separator + scriptName;
        validateScript(scriptPath);

        // 构建进程：使用bash执行脚本
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath);
        pb.directory(new File(scriptsDir));
        pb.redirectErrorStream(true);

        final Process process = pb.start();
        final StringBuilder output = new StringBuilder();
        // shouldStop[0]: checker 判定应提前停止的标志
        final boolean[] shouldStop = {false};

        // 启动后台线程读取输出，避免缓冲区满导致阻塞
        Thread outputReader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), "UTF-8"));
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            synchronized (output) {
                                output.append(line).append("\n");
                            }
                            // 如果有 checker，检查是否满足提前停止条件
                            if (checker != null) {
                                String currentOutput;
                                synchronized (output) {
                                    currentOutput = output.toString();
                                }
                                if (checker.shouldStop(currentOutput)) {
                                    shouldStop[0] = true;
                                    process.destroy();
                                    break;
                                }
                            }
                        }
                    } finally {
                        reader.close();
                    }
                } catch (IOException e) {
                    // 进程被终止时可能抛出异常，忽略即可
                }
            }
        }, "output-reader-" + scriptName);
        outputReader.setDaemon(true);
        outputReader.start();

        // 主线程显示进度条
        ProgressDisplay progress = new ProgressDisplay(durationSeconds);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationSeconds * 1000L;

        while (System.currentTimeMillis() < endTime) {
            // 检查进程是否已提前退出（含 checker 触发的销毁）
            if (!process.isAlive() || shouldStop[0]) {
                break;
            }
            int elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000);
            progress.display(elapsed);
            Thread.sleep(1000);
        }

        // 进度到100%
        progress.display(durationSeconds);
        System.out.println(); // 换行

        if (shouldStop[0]) {
            System.out.println("  ⚡ 已达到记录数上限，提前停止采集");
        }

        // 终止进程（tcpdump类脚本不会自行退出）
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        process.waitFor(5, TimeUnit.SECONDS);

        // 等待输出读取线程结束
        outputReader.join(3000);

        return output.toString();
    }

    /**
     * 校验脚本文件是否存在
     *
     * @param scriptPath 脚本文件的完整路径
     * @throws FileNotFoundException 脚本文件不存在时抛出
     */
    void validateScript(String scriptPath) throws FileNotFoundException {
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new FileNotFoundException("脚本文件不存在: " + scriptPath);
        }
        if (!scriptFile.canRead()) {
            throw new FileNotFoundException("脚本文件无法读取: " + scriptPath);
        }
    }

    /**
     * 获取脚本目录路径
     *
     * @return 脚本目录路径
     */
    public String getScriptsDir() {
        return scriptsDir;
    }
}
