package com.hsj.hmdp.config;

import com.hsj.hmdp.pojo.ShopType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate MyStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 创建 StringRedisTemplate 实例
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory); // 设置 Redis 连接工厂
        return template;
    }
}
