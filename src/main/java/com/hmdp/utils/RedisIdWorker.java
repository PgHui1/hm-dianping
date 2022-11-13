package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/*Redis实现全局唯一Id*/
    /*
        此处不使用UUID，全局唯一Id需要满足以下条件:
        递增:方便建立索引
        唯一:确保订单无重复
        高可用:数量要足够多
        高性能‘
        安全性:规律性不强
        结论，使用long 64位，蒂1位固定为0，代表符号位,后31位代表时间戳，再后面32位使用redis自增数值，
    * */
@Component
public class RedisIdWorker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private final static long BEGIN_TIMESTAMP = 1640995200L;

    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2.生成序列号
        // 2.1.获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3.拼接并返回
        return timestamp << 32 | count;
    }

    public static void main(String[] args) {
        RedisIdWorker redisIdWorker = new RedisIdWorker();
        redisIdWorker.nextId("order");
        //redisIdWorker.nextId("");
    }
}
