package com.netscan;

/**
 * 控制台进度条显示器
 * 在控制台上显示一个动态刷新的进度条，指示任务执行进度
 */
public class ProgressDisplay {

    /** 进度条总宽度（字符数） */
    private static final int BAR_WIDTH = 40;

    /** 任务总时长（秒） */
    private final int totalSeconds;

    /** 旋转指示器的字符集 */
    private static final char[] SPINNER = {'|', '/', '-', '\\'};

    /**
     * 构造进度显示器
     *
     * @param totalSeconds 任务总时长（秒）
     */
    public ProgressDisplay(int totalSeconds) {
        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("总时长必须大于0");
        }
        this.totalSeconds = totalSeconds;
    }

    /**
     * 显示当前进度
     * 使用回车符(\r)覆盖当前行实现动态刷新
     *
     * @param elapsedSeconds 已过去的秒数
     */
    public void display(int elapsedSeconds) {
        // 确保不超过100%
        int elapsed = Math.min(elapsedSeconds, totalSeconds);
        int percent = (int) ((elapsed * 100L) / totalSeconds);
        int filled = (int) ((elapsed * (long) BAR_WIDTH) / totalSeconds);
        int remaining = BAR_WIDTH - filled;

        // 计算剩余时间
        int remainingTime = totalSeconds - elapsed;
        int mins = remainingTime / 60;
        int secs = remainingTime % 60;

        // 旋转指示器
        char spinner = SPINNER[elapsed % SPINNER.length];

        // 构建进度条字符串
        StringBuilder bar = new StringBuilder();
        bar.append("  ").append(spinner).append(" [");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        for (int i = 0; i < remaining; i++) {
            bar.append("░");
        }
        bar.append("] ");
        bar.append(String.format("%3d%%", percent));
        bar.append(String.format("  剩余 %02d:%02d", mins, secs));

        // 使用\r回到行首覆盖写入
        System.out.print("\r" + bar.toString());
        System.out.flush();
    }

    /**
     * 生成进度条字符串（不输出到控制台，供测试使用）
     *
     * @param elapsedSeconds 已过去的秒数
     * @return 进度条字符串
     */
    public String getProgressString(int elapsedSeconds) {
        int elapsed = Math.min(elapsedSeconds, totalSeconds);
        int percent = (int) ((elapsed * 100L) / totalSeconds);
        int filled = (int) ((elapsed * (long) BAR_WIDTH) / totalSeconds);
        int remaining = BAR_WIDTH - filled;

        int remainingTime = totalSeconds - elapsed;
        int mins = remainingTime / 60;
        int secs = remainingTime % 60;

        char spinner = SPINNER[elapsed % SPINNER.length];

        StringBuilder bar = new StringBuilder();
        bar.append("  ").append(spinner).append(" [");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        for (int i = 0; i < remaining; i++) {
            bar.append("░");
        }
        bar.append("] ");
        bar.append(String.format("%3d%%", percent));
        bar.append(String.format("  剩余 %02d:%02d", mins, secs));

        return bar.toString();
    }

    /**
     * 获取总时长
     *
     * @return 总时长（秒）
     */
    public int getTotalSeconds() {
        return totalSeconds;
    }
}
