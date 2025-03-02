package com.hsj.hmdp.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

//懒汉式单例类，用于创建雪花ID
public class SnowflakeSingleton {
    private static volatile Snowflake snowflake; //防止指令重排导致synchronized失效

    //私有化构造方法，防止外部实例化
    private SnowflakeSingleton() {}

    public static Snowflake getSnowflake() {
        //DCL双重检查锁防止多线程破坏单例
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
