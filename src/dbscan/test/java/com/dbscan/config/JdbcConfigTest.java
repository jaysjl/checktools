package com.dbscan.config;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JdbcConfig 单元测试
 */
public class JdbcConfigTest {

    @Test
    public void testConstructorWithParameters() {
        JdbcConfig config = new JdbcConfig("MySQL", "jdbc:mysql://localhost:3306/test", "user", "password");
        
        assertEquals("MySQL", config.getType());
        assertEquals("jdbc:mysql://localhost:3306/test", config.getUrl());
        assertEquals("user", config.getUsername());
        assertEquals("password", config.getPassword());
    }

    @Test
    public void testEmptyConstructor() {
        JdbcConfig config = new JdbcConfig();
        assertNull(config.getType());
        assertNull(config.getUrl());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    @Test
    public void testSettersAndGetters() {
        JdbcConfig config = new JdbcConfig();
        config.setType("PostgreSQL");
        config.setUrl("jdbc:postgresql://localhost:5432/test");
        config.setUsername("postgres");
        config.setPassword("secret");

        assertEquals("PostgreSQL", config.getType());
        assertEquals("jdbc:postgresql://localhost:5432/test", config.getUrl());
        assertEquals("postgres", config.getUsername());
        assertEquals("secret", config.getPassword());
    }

    @Test
    public void testSQLServerConfig() {
        JdbcConfig config = new JdbcConfig("SQLServer", "jdbc:sqlserver://localhost:1433;databaseName=test", "sa", "Pass@1234");
        
        assertEquals("SQLServer", config.getType());
        assertTrue(config.getUrl().contains("sqlserver"));
        assertEquals("sa", config.getUsername());
    }

    @Test
    public void testOracleConfig() {
        JdbcConfig config = new JdbcConfig("Oracle", "jdbc:oracle:thin:@localhost:1521:orcl", "scott", "tiger");
        
        assertEquals("Oracle", config.getType());
        assertTrue(config.getUrl().contains("oracle"));
        assertEquals("scott", config.getUsername());
    }
}
