package com.hmdp.utils;

public interface Ilock {
     boolean tryLock(String serviceName);

     void unlock(String serviceName);
}
