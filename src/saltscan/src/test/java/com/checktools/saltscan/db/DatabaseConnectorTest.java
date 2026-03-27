package com.checktools.saltscan.db;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.checktools.saltscan.config.ConfigManager;

/**
 * 数据库连接器单元测试
 */
public class DatabaseConnectorTest {
    
    private DatabaseConnector connector;
    private ConfigManager mockConfigManager;

    @Before
    public void setUp() {
        mockConfigManager = mock(ConfigManager.class);
        connector = new DatabaseConnector(mockConfigManager);
    }

    @Test
    public void testConstructor() {
        assertNotNull(connector);
    }

    @Test
    public void testGetConnection() {
        assertNull(connector.getConnection());
    }

    @Test
    public void testDatabaseTypeLoading() {
        // 测试MySQL驱动加载
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            fail("MySQL驱动加载失败");
        }
    }

    @Test
    public void testPostgresqlDriverLoading() {
        // 测试PostgreSQL驱动加载
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            fail("PostgreSQL驱动加载失败");
        }
    }

    @Test
    public void testSqlServerDriverLoading() {
        // 测试SQL Server驱动加载
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            fail("SQL Server驱动加载失败");
        }
    }

    @Test
    public void testUnsupportedDatabaseType() {
        // 测试不支持的数据库类型
        Exception exception = null;
        try {
            Class.forName("oracle.jdbc.driver.UnsupportedDriver");
        } catch (ClassNotFoundException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void testQueryColumnNameParsing() {
        String tableName = "schema.table";
        String[] parts = tableName.split("\\.");
        assertEquals(2, parts.length);
        assertEquals("schema", parts[0]);
        assertEquals("table", parts[1]);
    }

    @Test
    public void testSimpleTableNameParsing() {
        String tableName = "table";
        String[] parts = tableName.split("\\.");
        assertEquals(1, parts.length);
        assertEquals("table", parts[0]);
    }
}
