package com.hsj.hmdp.utils;

import java.util.regex.Pattern;

public class RegexUtils {
    /**
     * 检查字符串是否不符合正则表达式
     * @param input 输入的字符串
     * @param regex 正则表达式
     * @return true 如果不匹配，false 如果匹配
     */
    public static boolean mismatch(String input, String regex) {
        // 如果输入为 null 或正则为 null，认为不匹配
        if (input == null || regex == null) {return true;}

        // 使用 Pattern 和 matches 检查是否匹配正则
        return !Pattern.matches(regex, input);
    }
    //判断一个手机号是否合法 true：非法 false：合法
    public static boolean isPhoneInvalid(String phone) {
        return mismatch(phone,Constants.PHONE_REGEX);
    }
}
