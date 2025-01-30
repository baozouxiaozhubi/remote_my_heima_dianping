package Mapper.utils;

public final class Constants {
    public static final String CODE = "code";
    public static final String USER = "user";
    public static final String LOGIN_CODE_HEADER = "login:code:";
    public static final String LOGIN_TOKEN_HEADER = "login:token:";
    public static final String CACHE_SHOP = "cache:shop:";
    public static final String IDWORKER_HEADER = "icr:";
    public static final String CACHE_SHOP_TYPE = "cache:shop:type";
    public static final String REDIS_LOCK = "cache:shop:lock";
    public static final String EMPTY_KEY = "empty";
    public static final String EMPTY_VALUE = "empty";
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
    public static final String IMAGE_UPLOAD_DIR = "E:\\hmdp\\nginx-1.18.0\\html\\hmdp\\imgs";
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final String ID_PREFIX_ORDER = "order";

    public static final Long LOGIN_TOKEN_TTL = 60L; //用于登陆的验证码的token过期时间
    public static final Long CACHE_SHOP_TTL = 30L;
    public static final Long CACHE_SHOP_NULL_TTL = 3L;
    public static final Long REDIS_SETNX_TTL = 10L;///用于防止缓存穿透的空值的过期时间
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;
    public static final Long LOGIN_CODE_TTL = 5L;
    public static final Long WORKER_ID = 1L;
    public static final Long DATACENTER_ID = 1L;

    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;
}
