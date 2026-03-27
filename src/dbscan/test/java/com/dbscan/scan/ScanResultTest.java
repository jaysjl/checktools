package com.dbscan.scan;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * ScanResult 单元测试
 */
public class ScanResultTest {

    private ScanResult scanResult;

    @Before
    public void setUp() {
        scanResult = new ScanResult(
                "192.168.1.1",
                3306,
                "MySQL",
                "dbscan_test",
                "public",
                "users",
                "phone",
                100L,
                "phone",
                "手机号",
                50.0,
                "13800138000; 13800138001"
        );
    }

    @Test
    public void testConstructorWithParameters() {
        assertEquals("192.168.1.1", scanResult.getIp());
        assertEquals(Integer.valueOf(3306), scanResult.getPort());
        assertEquals("MySQL", scanResult.getDatabaseType());
        assertEquals("dbscan_test", scanResult.getDatabase());
        assertEquals("public", scanResult.getSchema());
        assertEquals("users", scanResult.getTable());
        assertEquals("phone", scanResult.getColumn());
        assertEquals(100L, (long) scanResult.getExtractCount());
        assertEquals("phone", scanResult.getRuleName());
        assertEquals("手机号", scanResult.getRuleDescription());
        assertEquals(50.0, scanResult.getMatchRate(), 0.01);
        assertEquals("13800138000; 13800138001", scanResult.getSamples());
    }

    @Test
    public void testEmptyConstructor() {
        ScanResult result = new ScanResult();
        assertNull(result.getSchema());
        assertNull(result.getTable());
        assertNull(result.getColumn());
    }

    @Test
    public void testGettersAndSetters() {
        ScanResult result = new ScanResult();
        result.setIp("192.168.1.2");
        result.setPort(5432);
        result.setDatabaseType("PostgreSQL");
        result.setDatabase("test_db");
        result.setSchema("test");
        result.setTable("test_table");
        result.setColumn("test_column");
        result.setExtractCount(100L);
        result.setRuleName("test_rule");
        result.setRuleDescription("test description");
        result.setMatchRate(75.5);
        result.setSamples("value1; value2");

        assertEquals("192.168.1.2", result.getIp());
        assertEquals(Integer.valueOf(5432), result.getPort());
        assertEquals("PostgreSQL", result.getDatabaseType());
        assertEquals("test_db", result.getDatabase());
        assertEquals("test", result.getSchema());
        assertEquals("test_table", result.getTable());
        assertEquals("test_column", result.getColumn());
        assertEquals(100L, (long) result.getExtractCount());
        assertEquals("test_rule", result.getRuleName());
        assertEquals("test description", result.getRuleDescription());
        assertEquals(75.5, result.getMatchRate(), 0.01);
        assertEquals("value1; value2", result.getSamples());
    }

    @Test
    public void testToString() {
        String str = scanResult.toString();
        assertTrue(str.contains("192.168.1.1"));
        assertTrue(str.contains("public"));
        assertTrue(str.contains("users"));
        assertTrue(str.contains("phone"));
    }
}
