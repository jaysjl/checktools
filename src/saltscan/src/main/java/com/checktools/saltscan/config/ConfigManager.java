package com.checktools.saltscan.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理器 - 处理JSON格式的配置文件
 */
public class ConfigManager {
    private final JsonObject configJson;

    public ConfigManager(String configPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(configPath)));
        this.configJson = new Gson().fromJson(content, JsonObject.class);
    }

    public String getJdbcType() {
        return getNestedString("jdbc.type");
    }

    public String getJdbcUrl() {
        return getNestedString("jdbc.url");
    }

    public String getJdbcUsername() {
        return getNestedString("jdbc.username");
    }

    public String getJdbcPassword() {
        return getNestedString("jdbc.password");
    }

    public String getJdbcIp() {
        return getNestedString("jdbc.ip");
    }

    public int getJdbcPort() {
        JsonObject jdbc = configJson.getAsJsonObject("jdbc");
        return jdbc != null && jdbc.has("port") ? jdbc.get("port").getAsInt() : 3306;
    }

    public String getJdbcDatabase() {
        return getNestedString("jdbc.database");
    }

    public String getJdbcSchema() {
        return getNestedString("jdbc.schema");
    }

    /**
     * 获取所有扫描目标列表
     */
    public List<Map<String, Object>> getScanTargets() {
        List<Map<String, Object>> targets = new ArrayList<>();
        JsonObject scan = configJson.getAsJsonObject("scan");
        
        if (scan != null && scan.has("targets")) {
            JsonArray targetsArray = scan.getAsJsonArray("targets");
            for (JsonElement element : targetsArray) {
                JsonObject targetObj = element.getAsJsonObject();
                Map<String, Object> target = new HashMap<>();
                
                // 获取表名
                String table = targetObj.has("table") ? targetObj.get("table").getAsString() : null;
                target.put("table", table);
                
                // 获取列列表
                List<String> columns = new ArrayList<>();
                if (targetObj.has("columns")) {
                    JsonArray columnsArray = targetObj.getAsJsonArray("columns");
                    for (JsonElement col : columnsArray) {
                        columns.add(col.getAsString());
                    }
                }
                target.put("columns", columns);
                
                targets.add(target);
            }
        }
        
        return targets;
    }

    public int getScanLimit() {
        JsonObject scan = configJson.getAsJsonObject("scan");
        return scan != null && scan.has("limit") ? scan.get("limit").getAsInt() : 3000;
    }

    public String getOutputPath() {
        return getNestedString("output.path");
    }

    public String getProperty(String path) {
        try {
            String[] parts = path.split("\\.");
            JsonObject current = configJson;
            
            for (int i = 0; i < parts.length - 1; i++) {
                current = current.getAsJsonObject(parts[i]);
                if (current == null) return null;
            }
            
            if (current.has(parts[parts.length - 1])) {
                return current.get(parts[parts.length - 1]).getAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getNestedString(String path) {
        try {
            String[] parts = path.split("\\.");
            JsonObject current = configJson;
            
            for (int i = 0; i < parts.length - 1; i++) {
                current = current.getAsJsonObject(parts[i]);
                if (current == null) return null;
            }
            
            if (current.has(parts[parts.length - 1])) {
                return current.get(parts[parts.length - 1]).getAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
