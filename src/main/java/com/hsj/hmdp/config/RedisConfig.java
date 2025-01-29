package com.hsj.hmdp.config;

import com.hsj.hmdp.pojo.ShopType;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
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

    //工厂类，可以获得Redisson的各种工具
    @Bean
    public RedissonClient RedissonClient() {
        //新建配置类
        Config config = new Config();
        //添加redis地址，这里添加的是单点地址，可以通过config.useClusterServers()添加集群的地址
        config.useSingleServer().setAddress("redis://121.40.226.231:6379").setPassword("123456");
        return Redisson.create(config);
    }
}
