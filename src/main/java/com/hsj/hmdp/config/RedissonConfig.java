package com.hsj.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 创建 Redisson 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://121.40.226.231:6379") // 设置 Redis 服务器地址
                .setPassword("123456") // 如果有密码则设置
                .setDatabase(0) // 选择 Redis 数据库
                .setConnectionPoolSize(5) // 最大连接数
                .setConnectionMinimumIdleSize(2); // 不能超过 connectionPoolSize

        return Redisson.create(config);
    }
}
