package Mapper.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1577836800;
    private static final int COUNT_BITS = 32; //序列号的位数

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //全局唯一Id生成器 参数是业务前缀
    public Long nextId(String keyPrefix) {
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long currentTimestamp = now.toEpochSecond(ZoneOffset.UTC)-BEGIN_TIMESTAMP;

        //2.生成序列号--由于单个key增长会有上限值(2^64)，所以添加后缀加一个时间日期(精确到天)，以作区分
        // 即每个业务每一天都有一个自增ID池，这样做既可以防止达到上线又起到了记录的效果
        String now_day = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment(Constants.IDWORKER_HEADER+keyPrefix+":"+now_day);

        //3.拼接并返回(借助位运算)
        return currentTimestamp << COUNT_BITS | count;
    }

    public static void main(String[] args) {
        //起始时间
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 1, 0, 0);
        System.out.println("second="+ldt.toEpochSecond(ZoneOffset.UTC));
    }
}
