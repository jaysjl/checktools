package com.dbscan.config;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 配置加载器，从 JSON 文件加载配置
 */
public class ConfigLoader {
    private static final Gson gson = new Gson();

    /**
     * 从 JSON 文件加载配置
     * @param configPath 配置文件路径
     * @return 配置对象
     * @throws IOException 文件读取异常
     */
    public static Config loadConfig(String configPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(configPath)), StandardCharsets.UTF_8);
        return gson.fromJson(content, Config.class);
    }

    /**
     * 验证配置的有效性
     * @param config 配置对象
     * @throws IllegalArgumentException 配置不合法
     */
    public static void validateConfig(Config config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("配置对象不能为空");
        }

        // 验证 JDBC 配置
        if (config.getJdbc() == null) {
            throw new IllegalArgumentException("JDBC 配置不能为空");
        }
        if (config.getJdbc().getUrl() == null || config.getJdbc().getUrl().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL 不能为空");
        }
        if (config.getJdbc().getUsername() == null || config.getJdbc().getUsername().isEmpty()) {
            throw new IllegalArgumentException("数据库用户名不能为空");
        }

        // 验证扫描配置
        if (config.getScan() == null) {
            throw new IllegalArgumentException("扫描配置不能为空");
        }
        if (config.getScan().getTargets() == null || config.getScan().getTargets().isEmpty()) {
            throw new IllegalArgumentException("扫描目标不能为空");
        }
        if (config.getScan().getRules() == null || config.getScan().getRules().isEmpty()) {
            throw new IllegalArgumentException("扫描规则不能为空");
        }

        // 验证输出配置
        if (config.getOutput() == null) {
            throw new IllegalArgumentException("输出配置不能为空");
        }
        if (config.getOutput().getPath() == null || config.getOutput().getPath().isEmpty()) {
            throw new IllegalArgumentException("输出路径不能为空");
        }
    }
}
