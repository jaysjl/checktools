package com.dbscan.config;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

/**
 * ConfigLoader 单元测试
 */
public class ConfigLoaderTest {

    @Test
    public void testValidateConfigWithNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigLoader.validateConfig(null);
        });
    }

    @Test
    public void testValidateConfigWithMissingJdbc() {
        Config config = new Config();
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigLoader.validateConfig(config);
        });
    }

    @Test
    public void testValidateConfigWithEmptyJdbcUrl() {
        Config config = new Config();
        JdbcConfig jdbc = new JdbcConfig();
        jdbc.setUrl("");
        config.setJdbc(jdbc);
        
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigLoader.validateConfig(config);
        });
    }

    @Test
    public void testValidateConfigWithValidJdbc() {
        Config config = new Config();
        JdbcConfig jdbc = new JdbcConfig("MySQL", "jdbc:mysql://localhost:3306/test", "user", "pass");
        config.setJdbc(jdbc);
        
        // 应该在 scan 配置校验时抛出异常，这里只是验证 JDBC 的验证通过
        assertThrows(IllegalArgumentException.class, () -> {
            ConfigLoader.validateConfig(config);
        });
    }

    @Test
    public void testValidateConfigWithValidConfig() {
        Config config = new Config();
        JdbcConfig jdbc = new JdbcConfig("MySQL", "jdbc:mysql://localhost:3306/test", "user", "pass");
        config.setJdbc(jdbc);

        ScanConfig scan = new ScanConfig();
        scan.setTargets(Arrays.asList("test_table"));
        ScanRule rule = new ScanRule("phone", "手机号", "^1[3-9]\\d{9}$");
        scan.setRules(Arrays.asList(rule));
        config.setScan(scan);

        OutputConfig output = new OutputConfig("./report.html");
        config.setOutput(output);

        // 验证应该通过
        try {
            ConfigLoader.validateConfig(config);
        } catch (IllegalArgumentException e) {
            fail("有效的配置不应该抛出异常: " + e.getMessage());
        }
    }

    private static void assertThrows(Class<? extends Exception> expectedType, Runnable runnable) {
        try {
            runnable.run();
            fail("期望抛出 " + expectedType.getSimpleName() + " 异常");
        } catch (Exception e) {
            assertTrue("期望异常类型为 " + expectedType.getSimpleName() + "，但实际为 " + e.getClass().getSimpleName(),
                    expectedType.isInstance(e));
        }
    }
}
