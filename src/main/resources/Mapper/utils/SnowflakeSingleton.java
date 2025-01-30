package Mapper.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

//懒汉式单例类，用于创建雪花ID
public class SnowflakeSingleton {
    private static volatile Snowflake snowflake;

    //私有化构造方法，防止外部实例化
    private SnowflakeSingleton() {}

    public static Snowflake getSnowflake() {
        if (snowflake == null) {
            synchronized (SnowflakeSingleton.class) {
                if (snowflake == null) {
                    snowflake = IdUtil.getSnowflake(Constants.WORKER_ID, Constants.DATACENTER_ID);
                }
            }
        }
        return snowflake;
    }
}
