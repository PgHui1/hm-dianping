
--比较操作数据的线程ID和当前锁的线程ID是否一致
--一致则删除，否则返回0

local lockId = redis.call('GET',KEYS[1]) -- get lock:order:userId

if (lockId == ARGV[1]) then
    return redis.call('DEL',KEYS[1])
end
    return 0
