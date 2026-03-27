package com.dbscan.config;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ScanRule 单元测试
 */
public class ScanRuleTest {

    @Test
    public void testConstructorWithParameters() {
        ScanRule rule = new ScanRule("phone", "手机号", "^1[3-9]\\d{9}$");
        
        assertEquals("phone", rule.getName());
        assertEquals("手机号", rule.getDescription());
        assertEquals("^1[3-9]\\d{9}$", rule.getRegex());
    }

    @Test
    public void testEmptyConstructor() {
        ScanRule rule = new ScanRule();
        assertNull(rule.getName());
        assertNull(rule.getDescription());
        assertNull(rule.getRegex());
    }

    @Test
    public void testSettersAndGetters() {
        ScanRule rule = new ScanRule();
        rule.setName("id_card");
        rule.setDescription("身份证号");
        rule.setRegex("\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9X]");

        assertEquals("id_card", rule.getName());
        assertEquals("身份证号", rule.getDescription());
        assertTrue(rule.getRegex().contains("18|19|20"));
    }

    @Test
    public void testPhoneRule() {
        ScanRule rule = new ScanRule("phone", "手机号", "^(?:0|86|\\+?86)?1[3-9]\\d{9}$");
        
        assertEquals("phone", rule.getName());
        assertTrue(rule.getRegex().contains("1[3-9]"));
    }

    @Test
    public void testIdRule() {
        ScanRule rule = new ScanRule("id", "身份证号", "\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])");
        
        assertEquals("id", rule.getName());
        assertTrue(rule.getRegex().contains("18|19|20"));
    }
}
