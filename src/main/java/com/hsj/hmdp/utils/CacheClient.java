package com.hsj.hmdp.utils;

// 基于StringRedisTemplate封装一个缓存工具类CacheClient，满足下列需求:
// 方法1:将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
// 方法2:将任意java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
// 方法3:根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
// 方法4:根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    // 创建 JSON 配置
    private static final JSONConfig jsonConfig = new JSONConfig().setDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //用于【逻辑删除】解决缓存击穿问题的线程池(当发现查询的数据已经逻辑过期时，通过新线程来执行重建缓存)
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    //方法1:将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
    public void setWithTTL(String key, Object value, Long Time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value,jsonConfig), Time, timeUnit);
    }

    // 方法2:将任意java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
    public void setWithLogicalTTL(String key, Object value, Long Time, TimeUnit timeUnit) {
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(Time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData,jsonConfig), Time, timeUnit);
    }

    // 方法3:根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题(没有解决缓存击穿)
    public <R,ID> R queryWithTTL(String keyPrefix, ID id, Class<R> type,
                                 Function<ID,R> function, Long time, TimeUnit timeUnit)
    {
        String key = keyPrefix + id;
        //查看缓存中有没有
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)) //isNotBlank(),对于null或空串都返回false
        {
            return JSONUtil.toBean(json,type);
        }

        if(json != null) return null; //==null说明未命中，应该从数据库中查询，!=null说明是”“空串,即占位符，代表缓存穿透

        //从数据库中查询，由于不知道根据什么查询，所以应该由调用者指定查询逻辑
        R r = function.apply(id);

        //不存在 缓存穿透，存空值
        if(r==null)
        {
            stringRedisTemplate.opsForValue().set(key,"",3,TimeUnit.MINUTES);
            return null;
        }

        //存在 写入缓存
        this.setWithTTL(key,r,time,timeUnit);
        return r;
    }

    // 方法4:根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
    public <R,ID> R queryWithLogicalTTL(String keyPrefix, ID id, Class<R> type,
                                        Function<ID,R> function, Long time, TimeUnit timeUnit)
    {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(json))
        {
            //缓存不存在(这里因为缓存击穿问题只发生在热点数据上，一般缓存【从未出现】的数据代表数据不重要
            // 例如未参与活动)，因此只需要解决缓存击穿问题，不需要考虑缓存穿透问题
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json,RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //由于RedisData中的data为Object类型，需要单独序列化成JSON再反序列化成特定类型
        R r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        //判断是否逻辑过期
        if(expireTime.isAfter(LocalDateTime.now()))
        {
            //逻辑未过期则直接返回
            return r;
        }
        //逻辑过期则开启新线程进行缓存重建
        String lockKey = keyPrefix+":LOCK"+id;
        boolean isLock = tryGetLock(lockKey);
        if(isLock)
        {
            try
            {
                //查询数据库
                R r1 = function.apply(id);
                //写入Redis
                this.setWithLogicalTTL(key,r1,time,timeUnit);
            }
            catch(Exception e)
            {
                throw new RuntimeException(e);
            }
            finally {
                unlock(lockKey);
            }
        }
        return r;
    }

    //使用互斥锁解决缓存击穿问题(考虑缓存穿透)
    public <R,ID> R queryWithMutex(String prefix, ID id, Class<R> type,
                                   Function<ID,R> function, Long time, TimeUnit timeUnit)
    {
        String key = prefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json))
        {
            //击中则直接返回
            return JSONUtil.toBean(json,type);
        }
        //判断是否发生缓存穿透
        if(json != null) return null;
        //未击中则从数据库中查询
        String lockKey = prefix+":LOCK"+id;
        R r = null;
        try
        {
            boolean isLock = tryGetLock(lockKey);
            if(!isLock)
            {
                Thread.sleep(50);
                return queryWithMutex(prefix,id,type,function,time,timeUnit);
            }
            r = function.apply(id);
            if(r==null)
            {
                //处理缓存穿透问题
                stringRedisTemplate.opsForValue().set(key,"",3,TimeUnit.MINUTES);
                return null;
            }
            this.setWithTTL(key,r,time,timeUnit);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            this.unlock(lockKey);
        }
        return r;
    }

    //利用Redis的setnx(方法名为setIfAbsent)命令实现分布式锁
    private boolean tryGetLock(String key)
    {
        //设置互斥锁有效期为10s，意为10s内完成缓存重建
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",Constants.REDIS_SETNX_TTL,TimeUnit.SECONDS);
        //使用BooleanUtil.isTrue防止拆箱时出现空指针
        return BooleanUtil.isTrue(flag);
    }

    //解锁，释放锁
    private void unlock(String key)
    {
        stringRedisTemplate.delete(key);
    }
}
