package com.checktools.filescan;

import com.checktools.filescan.model.SensitiveData;
import com.checktools.filescan.model.SensitiveData.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据检测器
 * 负责在可见字符文本中检测身份证号、手机号、邮箱等敏感数据
 */
public class SensitiveDataDetector {

    /**
     * 身份证号正则表达式
     * 18位身份证：6位地区码 + 8位出生日期 + 3位顺序码 + 1位校验码
     * 15位身份证：6位地区码 + 6位出生日期 + 3位顺序码
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "(?<![0-9])"  // 前面不能是数字（负向后顾）
            + "("
            + "[1-9]\\d{5}"                              // 6位地区码
            + "(?:19|20)\\d{2}"                           // 4位年份（19xx或20xx）
            + "(?:0[1-9]|1[0-2])"                         // 2位月份
            + "(?:0[1-9]|[12]\\d|3[01])"                  // 2位日期
            + "\\d{3}"                                    // 3位顺序码
            + "[0-9Xx]"                                   // 1位校验码
            + "|"
            + "[1-9]\\d{5}"                               // 15位身份证: 6位地区码
            + "\\d{2}"                                    // 2位年份
            + "(?:0[1-9]|1[0-2])"                         // 2位月份
            + "(?:0[1-9]|[12]\\d|3[01])"                  // 2位日期
            + "\\d{3}"                                    // 3位顺序码
            + ")"
            + "(?!\\d)"   // 后面不能是数字（负向前瞻）
    );

    /**
     * 手机号正则表达式
     * 匹配中国大陆11位手机号，以1开头，第二位为3-9
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<![0-9])"  // 前面不能是数字
            + "(1[3-9]\\d{9})"
            + "(?!\\d)"   // 后面不能是数字
    );

    /**
     * 邮箱正则表达式
     * 匹配常见邮箱格式：用户名@域名
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})"
    );

    /**
     * 检测文本中的所有敏感数据
     *
     * @param text 要检测的文本（可见字符）
     * @return 检测到的敏感数据列表
     */
    public List<SensitiveData> detect(String text) {
        List<SensitiveData> results = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return results;
        }

        // 依次检测各类敏感数据
        detectByPattern(text, ID_CARD_PATTERN, DataType.ID_CARD, results);
        detectByPattern(text, PHONE_PATTERN, DataType.PHONE, results);
        detectByPattern(text, EMAIL_PATTERN, DataType.EMAIL, results);

        return results;
    }

    /**
     * 使用正则表达式在文本中查找敏感数据
     *
     * @param text    要检测的文本
     * @param pattern 正则表达式
     * @param type    敏感数据类型
     * @param results 结果列表
     */
    private void detectByPattern(String text, Pattern pattern, DataType type,
                                  List<SensitiveData> results) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matched = matcher.group(1);
            int startIndex = matcher.start(1);

            // 对身份证号进行校验码验证（18位）
            if (type == DataType.ID_CARD && matched.length() == 18) {
                if (!validateIdCardChecksum(matched)) {
                    continue; // 校验码不通过，跳过
                }
            }

            results.add(new SensitiveData(type, matched, startIndex));
        }
    }

    /**
     * 验证18位身份证号的校验码
     * 根据GB 11643-1999标准计算校验码
     *
     * @param idCard 18位身份证号
     * @return 校验码是否正确
     */
    static boolean validateIdCardChecksum(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }

        // 加权因子
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        // 校验码对应值
        char[] checkChars = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = idCard.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            sum += (c - '0') * weights[i];
        }

        char expectedCheck = checkChars[sum % 11];
        char actualCheck = Character.toUpperCase(idCard.charAt(17));

        return expectedCheck == actualCheck;
    }
}
