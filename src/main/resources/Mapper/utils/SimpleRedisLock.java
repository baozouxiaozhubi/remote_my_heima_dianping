package Mapper.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private StringRedisTemplate stringRedisTemplate;

    private String name = "yryd:"; //业务名称
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";//每个服务器都有独立的JVM，维护独立的常量栈，堆，主进程池，所以在类中定义static final常量即可以实现JVM唯一标识
    //提前读取Lua脚本对象
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static
    {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock() {}
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //以当前线程的Id作为锁的值---目的：防止阻塞误删导致的线程安全问题
        //仍然有误删风险，因为线程Id是一个递增数字，在集群模式下多个服务器可能产生同一个线程Id
        //改进 添加一段UUID用于表示JVM唯一标识
        String ThreadId = ID_PREFIX + Thread.currentThread().getId();
        //获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name, ThreadId, timeoutSec, TimeUnit.SECONDS);//封装类可能为null
        return BooleanUtil.isTrue(success); //返回基本类型只需要true/false--防止自动拆箱，所以使用工具类主动拆箱后返回
    }

    //使用stringRedisTemplate调用Lua脚本，实现判断锁标识一致性和释放锁的原子性
    @Override
    public void unlock() {
        //调用Lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }
//    @Override
//    public void unlock() {
//        //获取JVM+线程唯一标识
//        String ThreadId = ID_PREFIX + Thread.currentThread().getId();
//        //获取锁标识
//        String LockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX+name);
//        //判断锁标识和JVM+线程唯一标识是否一致
//        if(ThreadId.equals(LockId)) {
//            //释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//        //否则什么都不做
//    }
}
