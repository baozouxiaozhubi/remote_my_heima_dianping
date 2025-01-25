package com.hsj.hmdp.utils;

public final class Constants {
    public static final String CODE = "code";
    public static final String USER = "user";
    public static final String LOGIN_CODE_HEADER = "login:code:";
    public static final String LOGIN_TOKEN_HEADER = "login:token:";
    public static final String CACHE_SHOP = "cache:shop:";
    public static final String CACHE_SHOP_TYPE = "cache:shop:type";
    public static final String REDIS_LOCK = "cache:shop:lock";
    public static final String EMPTY_KEY = "empty";
    public static final String EMPTY_VALUE = "empty";
    public static final int LOGIN_TOKEN_TTL = 5; //用于登陆的验证码的token过期时间
    public static final int CACHE_SHOP_TTL = 30;
    public static final int CACHE_SHOP_NULL_TTL = 3;
    public static final int REDIS_SETNX_TTL = 10;///用于防止缓存穿透的空值的过期时间
    public static final int CACHE_SHOP_TYPE_TTL = 30;
    public static final int LOGIN_CODE_TTL = 5;
    public static final int WORKER_ID = 1;
    public static final int DATACENTER_ID = 1;
}
