package com.hsj.hmdp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory);

        // 创建 ObjectMapper 用于 把Java对象序列化成JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 注册 Java8 时间类支持

        // 使用 Jackson2JsonRedisSerializer 作为序列化器(可以为不同数据类型选择不同的序列化器)--为Object选择序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 设置 RedisTemplate 的 key 和 value 序列化方式
        template.setKeySerializer(new StringRedisSerializer());  // key 使用 StringRedisSerializer
        template.setValueSerializer(jackson2JsonRedisSerializer);  // value 使用 Jackson2JsonRedisSerializer

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
