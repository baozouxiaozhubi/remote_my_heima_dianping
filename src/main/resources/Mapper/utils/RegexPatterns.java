package Mapper.utils;

//“系统变量”一半定义为抽象类 只能有static属性 且无法New对象
public abstract class RegexPatterns {
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
}
