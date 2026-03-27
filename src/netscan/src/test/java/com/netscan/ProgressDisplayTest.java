package com.netscan;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 进度条显示器测试
 */
public class ProgressDisplayTest {

    /**
     * 测试构造函数：正常参数
     */
    @Test
    public void testConstructor() {
        ProgressDisplay pd = new ProgressDisplay(60);
        assertEquals("总时长应为60秒", 60, pd.getTotalSeconds());
    }

    /**
     * 测试构造函数：非法参数应抛出异常
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZero() {
        new ProgressDisplay(0);
    }

    /**
     * 测试构造函数：负数应抛出异常
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegative() {
        new ProgressDisplay(-1);
    }

    /**
     * 测试进度0%时的显示
     */
    @Test
    public void testProgressAtStart() {
        ProgressDisplay pd = new ProgressDisplay(60);
        String result = pd.getProgressString(0);

        assertTrue("应包含0%", result.contains("0%"));
        assertTrue("应包含剩余时间01:00", result.contains("01:00"));
    }

    /**
     * 测试进度50%时的显示
     */
    @Test
    public void testProgressAtHalf() {
        ProgressDisplay pd = new ProgressDisplay(60);
        String result = pd.getProgressString(30);

        assertTrue("应包含50%", result.contains("50%"));
        assertTrue("应包含剩余时间00:30", result.contains("00:30"));
    }

    /**
     * 测试进度100%时的显示
     */
    @Test
    public void testProgressAtEnd() {
        ProgressDisplay pd = new ProgressDisplay(60);
        String result = pd.getProgressString(60);

        assertTrue("应包含100%", result.contains("100%"));
        assertTrue("应包含剩余时间00:00", result.contains("00:00"));
    }

    /**
     * 测试超过总时长时不应超过100%
     */
    @Test
    public void testProgressOverflow() {
        ProgressDisplay pd = new ProgressDisplay(60);
        String result = pd.getProgressString(120);

        assertTrue("不应超过100%", result.contains("100%"));
        assertTrue("剩余时间应为0", result.contains("00:00"));
    }

    /**
     * 测试进度条字符串包含进度条字符
     */
    @Test
    public void testProgressBarContainsBarChars() {
        ProgressDisplay pd = new ProgressDisplay(10);
        String result = pd.getProgressString(5);

        // 进度条应包含填充字符和未填充字符
        assertTrue("应包含进度条字符", result.contains("["));
        assertTrue("应包含进度条字符", result.contains("]"));
        assertTrue("应包含百分比", result.contains("%"));
    }

    /**
     * 测试小时长场景
     */
    @Test
    public void testShortDuration() {
        ProgressDisplay pd = new ProgressDisplay(5);
        String result = pd.getProgressString(3);

        assertTrue("应包含60%", result.contains("60%"));
        assertTrue("应包含剩余时间00:02", result.contains("00:02"));
    }
}
