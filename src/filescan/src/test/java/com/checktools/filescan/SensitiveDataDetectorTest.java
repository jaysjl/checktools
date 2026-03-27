package com.checktools.filescan;

import com.checktools.filescan.model.SensitiveData;
import com.checktools.filescan.model.SensitiveData.DataType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * SensitiveDataDetector 单元测试
 * 测试敏感数据检测器对身份证号、手机号、邮箱的检测能力
 */
public class SensitiveDataDetectorTest {

    private SensitiveDataDetector detector;

    @Before
    public void setUp() {
        detector = new SensitiveDataDetector();
    }

    // ==================== 手机号检测测试 ====================

    /**
     * 测试：检测标准11位手机号
     */
    @Test
    public void testDetectPhoneNumber() {
        List<SensitiveData> results = detector.detect("联系电话13912345678请拨打");

        boolean foundPhone = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.PHONE && data.getContent().equals("13912345678")) {
                foundPhone = true;
                break;
            }
        }
        assertTrue("应检测到手机号13912345678", foundPhone);
    }

    /**
     * 测试：检测多个手机号
     */
    @Test
    public void testDetectMultiplePhones() {
        List<SensitiveData> results = detector.detect("电话13800138000和15900159000");

        int phoneCount = 0;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.PHONE) {
                phoneCount++;
            }
        }
        assertEquals("应检测到2个手机号", 2, phoneCount);
    }

    /**
     * 测试：不应将非手机号的数字串误识别
     */
    @Test
    public void testNoFalsePositivePhone() {
        // 12位数字不应被识别为手机号
        List<SensitiveData> results = detector.detect("订单号123456789012");

        boolean foundPhone = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.PHONE) {
                foundPhone = true;
            }
        }
        assertFalse("12位数字不应被识别为手机号", foundPhone);
    }

    // ==================== 邮箱检测测试 ====================

    /**
     * 测试：检测标准邮箱地址
     */
    @Test
    public void testDetectEmail() {
        List<SensitiveData> results = detector.detect("请发送至test@example.com联系");

        boolean foundEmail = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.EMAIL && data.getContent().equals("test@example.com")) {
                foundEmail = true;
                break;
            }
        }
        assertTrue("应检测到邮箱test@example.com", foundEmail);
    }

    /**
     * 测试：检测包含数字和特殊字符的邮箱
     */
    @Test
    public void testDetectComplexEmail() {
        List<SensitiveData> results = detector.detect("邮箱user.name+tag@sub.domain.co.jp");

        boolean foundEmail = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.EMAIL) {
                foundEmail = true;
                break;
            }
        }
        assertTrue("应检测到复杂格式邮箱", foundEmail);
    }

    /**
     * 测试：检测多个邮箱
     */
    @Test
    public void testDetectMultipleEmails() {
        List<SensitiveData> results = detector.detect("联系a@b.com或者c@d.org");

        int emailCount = 0;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.EMAIL) {
                emailCount++;
            }
        }
        assertEquals("应检测到2个邮箱", 2, emailCount);
    }

    // ==================== 身份证号检测测试 ====================

    /**
     * 测试：检测18位身份证号（使用合法校验码）
     */
    @Test
    public void testDetectIdCard18() {
        // 110101199003074518 是一个校验码正确的身份证号示例
        // 计算: 1*7+1*9+0*10+1*5+0*8+1*4+1*2+9*1+9*6+0*3+0*7+3*9+0*10+7*5+4*8+5*4+1*2
        // = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+32+20+2 = 206
        // 206 % 11 = 8, checkChars[8] = '4', 最后一位应该是'8'...
        // 使用已知正确的身份证号: 11010119900307451X
        // 重新计算一个正确的:
        // 用 110101199003070011 来算:
        // 1*7+1*9+0*10+1*5+0*8+1*4+1*2+9*1+9*6+0*3+0*7+3*9+0*10+7*5+0*8+0*4+1*2
        // = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+0+0+2 = 154
        // 154 % 11 = 0, checkChars[0] = '1'
        // 所以 110101199003070011 的校验码应该是 1 -> 110101199003070011... 不对,最后一位已经是1了
        // 让我用 11010119900307001 + checkChars[sum%11]
        // 前17位: 11010119900307001
        // sum = 1*7+1*9+0*10+1*5+0*8+1*4+1*2+9*1+9*6+0*3+0*7+3*9+0*10+7*5+0*8+0*4+1*2
        // = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+0+0+2 = 154
        // 154%11=0, checkChars[0]='1'
        // 身份证号: 110101199003070011
        List<SensitiveData> results = detector.detect("身份证号110101199003070011请核实");

        boolean foundId = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.ID_CARD) {
                foundId = true;
                break;
            }
        }
        assertTrue("应检测到身份证号", foundId);
    }

    /**
     * 测试：检测身份证号（校验码为X）
     */
    @Test
    public void testDetectIdCardWithX() {
        // 前17位: 11010119900307771
        // sum = 1*7+1*9+0*10+1*5+0*8+1*4+1*2+9*1+9*6+0*3+0*7+3*9+0*10+7*5+7*8+7*4+1*2
        // = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+56+28+2 = 238
        // 238%11 = 7, checkChars[7] = '5'
        // 不是X... 让我找一个末尾是X的
        // 需要 sum%11 = 2, 因为checkChars[2] = 'X'
        // 前17位: 11010119900307003
        // sum = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+0+0+6 = 158
        // 158%11 = 4, checkChars[4]='8'
        // 前17位: 11010119900307079
        // sum = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+0+28+18 = 198
        // 198%11 = 0, checkChars[0]='1'
        // 前17位: 11010119900307091
        // sum = 7+9+0+5+0+4+2+9+54+0+0+27+0+35+0+36+2 = 190
        // 190%11 = 3, checkChars[3]='9'
        // 前17位: 53010119900307001
        // sum = 5*7+3*9+0*10+1*5+0*8+1*4+1*2+9*1+9*6+0*3+0*7+3*9+0*10+7*5+0*8+0*4+1*2
        // = 35+27+0+5+0+4+2+9+54+0+0+27+0+35+0+0+2 = 200
        // 200%11 = 2, checkChars[2]='X'
        // 身份证号: 53010119900307001X
        List<SensitiveData> results = detector.detect("身份证53010119900307001X请核实");

        boolean foundId = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.ID_CARD && data.getContent().contains("X")) {
                foundId = true;
                break;
            }
        }
        assertTrue("应检测到末尾为X的身份证号", foundId);
    }

    /**
     * 测试：校验码不正确的身份证号不应被检测到
     */
    @Test
    public void testRejectInvalidIdCardChecksum() {
        // 110101199003070012 - 正确校验码应为1，这里改为2
        List<SensitiveData> results = detector.detect("身份证号110101199003070012不合法");

        boolean foundId = false;
        for (SensitiveData data : results) {
            if (data.getType() == DataType.ID_CARD && data.getContent().equals("110101199003070012")) {
                foundId = true;
            }
        }
        assertFalse("校验码错误的身份证号不应被检测到", foundId);
    }

    /**
     * 测试：validateIdCardChecksum方法
     */
    @Test
    public void testValidateIdCardChecksum() {
        // 正确的身份证号
        assertTrue(SensitiveDataDetector.validateIdCardChecksum("110101199003070011"));
        assertTrue(SensitiveDataDetector.validateIdCardChecksum("53010119900307001X"));

        // 错误的身份证号
        assertFalse(SensitiveDataDetector.validateIdCardChecksum("110101199003070012"));
        assertFalse(SensitiveDataDetector.validateIdCardChecksum(null));
        assertFalse(SensitiveDataDetector.validateIdCardChecksum("12345"));
    }

    // ==================== 综合测试 ====================

    /**
     * 测试：空文本不应检测到任何敏感数据
     */
    @Test
    public void testDetectEmptyText() {
        List<SensitiveData> results = detector.detect("");
        assertTrue("空文本不应有敏感数据", results.isEmpty());
    }

    /**
     * 测试：null文本不应检测到任何敏感数据
     */
    @Test
    public void testDetectNullText() {
        List<SensitiveData> results = detector.detect(null);
        assertTrue("null文本不应有敏感数据", results.isEmpty());
    }

    /**
     * 测试：同时包含多种敏感数据的文本
     */
    @Test
    public void testDetectMixedSensitiveData() {
        String text = "姓名张三身份证110101199003070011手机13912345678邮箱zhangsan@test.com";
        List<SensitiveData> results = detector.detect(text);

        boolean hasIdCard = false;
        boolean hasPhone = false;
        boolean hasEmail = false;

        for (SensitiveData data : results) {
            switch (data.getType()) {
                case ID_CARD:
                    hasIdCard = true;
                    break;
                case PHONE:
                    hasPhone = true;
                    break;
                case EMAIL:
                    hasEmail = true;
                    break;
            }
        }

        assertTrue("应检测到身份证号", hasIdCard);
        assertTrue("应检测到手机号", hasPhone);
        assertTrue("应检测到邮箱", hasEmail);
    }

    /**
     * 测试：SensitiveData模型类
     */
    @Test
    public void testSensitiveDataModel() {
        SensitiveData data = new SensitiveData(DataType.PHONE, "13912345678", 10);

        assertEquals(DataType.PHONE, data.getType());
        assertEquals("13912345678", data.getContent());
        assertEquals(10, data.getStartIndex());
        assertEquals("手机号", data.getType().getDisplayName());
        assertNotNull(data.toString());
    }
}
