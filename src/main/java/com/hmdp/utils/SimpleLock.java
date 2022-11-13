package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class SimpleLock implements Ilock {
    private static final String KEY_PREFIX="lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    @Resource
    private StringRedisTemplate template;

    private static DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {

        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleLock(StringRedisTemplate template) {
        this.template = template;
    }

    @Override
    public boolean tryLock(String serviceName) {
        long id = Thread.currentThread().getId();
        Boolean aBoolean = template.opsForValue().setIfAbsent(KEY_PREFIX + serviceName, ID_PREFIX+id, 10L, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(aBoolean);
    }

    @Override
    public void unlock(String serviceName) {
        template.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + serviceName),
                ID_PREFIX + Thread.currentThread().getId()
                );
    }


    private void unlock2(String serviceName) {
        // 获取线程标示
        String threadId = KEY_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标示
        String id = template.opsForValue().get(KEY_PREFIX + serviceName);
        // 判断标示是否一致
        /*改进后的第二版
        * 判断和释放是两个步骤，所以此处没有原子性
        * 也就是说可能先判断成功进来了，但是要释放的时候因为gc等原因产生阻塞，进而造成超时释放
        * 最终导致再次误删，所以还需要改进
        * 改进的方案为使用lua脚本保证释放锁的原子性操作
        * */
        if(threadId.equals(id)) {
            // 释放锁
            template.delete(KEY_PREFIX + serviceName);
        }
    }
}
