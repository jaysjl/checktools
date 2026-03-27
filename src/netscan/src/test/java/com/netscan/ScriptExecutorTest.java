package com.netscan;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;

/**
 * 脚本执行器测试
 */
public class ScriptExecutorTest {

    private static final String TEST_SCRIPTS_DIR = System.getProperty("java.io.tmpdir")
            + File.separator + "netscan_test_scripts";
    private ScriptExecutor executor;

    @Before
    public void setUp() throws Exception {
        // 创建临时脚本目录
        new File(TEST_SCRIPTS_DIR).mkdirs();
        executor = new ScriptExecutor(TEST_SCRIPTS_DIR);
    }

    @After
    public void tearDown() {
        // 清理临时文件
        File dir = new File(TEST_SCRIPTS_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    /**
     * 测试脚本目录获取
     */
    @Test
    public void testGetScriptsDir() {
        assertEquals("脚本目录应正确返回", TEST_SCRIPTS_DIR, executor.getScriptsDir());
    }

    /**
     * 测试校验不存在的脚本应抛出FileNotFoundException
     */
    @Test(expected = FileNotFoundException.class)
    public void testValidateScriptNotFound() throws FileNotFoundException {
        executor.validateScript(TEST_SCRIPTS_DIR + File.separator + "nonexistent.sh");
    }

    /**
     * 测试校验存在的脚本应正常通过
     */
    @Test
    public void testValidateScriptExists() throws Exception {
        // 创建临时脚本文件
        File tempScript = new File(TEST_SCRIPTS_DIR, "test.sh");
        tempScript.createNewFile();
        tempScript.setReadable(true);

        // 不应抛出异常
        executor.validateScript(tempScript.getAbsolutePath());
    }

    /**
     * 测试执行简单脚本并获取实时输出
     */
    @Test
    public void testExecuteWithRealtimeOutput() throws Exception {
        // 创建一个简单的测试脚本
        File testScript = createTestScript("echo_test.sh", "#!/bin/bash\necho 'Hello NetScan'\necho 'Test Output'");

        String result = executor.executeWithRealtimeOutput("echo_test.sh");

        assertNotNull("结果不应为null", result);
        assertTrue("结果应包含Hello NetScan", result.contains("Hello NetScan"));
        assertTrue("结果应包含Test Output", result.contains("Test Output"));
    }

    /**
     * 测试执行带进度的脚本（短时长）
     */
    @Test
    public void testExecuteWithProgress() throws Exception {
        // 创建一个简单的测试脚本，输出一些内容后退出
        File testScript = createTestScript("progress_test.sh",
                "#!/bin/bash\necho 'progress output line 1'\necho 'progress output line 2'\nsleep 1");

        // 使用3秒时长，脚本会提前退出
        String result = executor.executeWithProgress("progress_test.sh", 3);

        assertNotNull("结果不应为null", result);
        assertTrue("结果应包含输出内容", result.contains("progress output line 1"));
    }

    /**
     * 测试带LineChecker的executeWithProgress：checker返回true时应提前停止
     */
    @Test
    public void testExecuteWithProgressAndLineChecker() throws Exception {
        // 创建脚本：持续输出行，每行一个编号
        StringBuilder scriptContent = new StringBuilder("#!/bin/bash\n");
        for (int i = 0; i < 20; i++) {
            scriptContent.append("echo 'line_").append(i).append("'\n");
            scriptContent.append("sleep 0.1\n");
        }
        createTestScript("checker_test.sh", scriptContent.toString());

        // LineChecker：当输出中出现 "line_5" 时停止
        ScriptExecutor.LineChecker checker = new ScriptExecutor.LineChecker() {
            @Override
            public boolean shouldStop(String currentOutput) {
                return currentOutput.contains("line_5");
            }
        };

        String result = executor.executeWithProgress("checker_test.sh", 30, checker);

        assertNotNull("结果不应为null", result);
        assertTrue("结果应包含 line_5", result.contains("line_5"));
    }

    /**
     * 测试executeWithProgress传null checker等同于无checker版本
     */
    @Test
    public void testExecuteWithProgressNullChecker() throws Exception {
        createTestScript("null_checker_test.sh",
                "#!/bin/bash\necho 'hello from null checker test'\nsleep 1");

        String result = executor.executeWithProgress("null_checker_test.sh", 3, null);

        assertNotNull("结果不应为null", result);
        assertTrue("结果应包含输出内容", result.contains("hello from null checker test"));
    }

    /**
     * 测试执行不存在的脚本应抛出异常
     */
    @Test(expected = FileNotFoundException.class)
    public void testExecuteNonExistentScript() throws Exception {
        executor.executeWithRealtimeOutput("nonexistent.sh");
    }

    /**
     * 辅助方法：创建测试脚本文件
     */
    private File createTestScript(String name, String content) throws Exception {
        File script = new File(TEST_SCRIPTS_DIR, name);
        java.io.FileWriter writer = new java.io.FileWriter(script);
        writer.write(content);
        writer.close();
        script.setExecutable(true);
        return script;
    }
}
