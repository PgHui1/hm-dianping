package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate template;

    /*
    * 数据库中的内容会改变，因而会造成数据库和缓存数据不一致，因此这里需要考虑更新策略
    * 策略有三：内存淘汰，超时剔除，主动更新
    * 这里选择主动更新
    * 这里做法为在进行更新等操作的时候，先修改数据库，再删除缓存
    * */
    @Override
    public Result queryById(Long id) {

       Shop shop =  queryWithMutex(id);

       if (shop == null){

           return Result.fail("商铺不存在");
       }

        return Result.ok(shop);
    }

    /*
    * 缓存更新策略
    * 主动更新
    * cache aside pattern
    * 先操作数据库，再删除缓存，防止对缓存无效的修改
    * */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺id不能为空");
        }
        this.updateById(shop);
        String key = CACHE_SHOP_KEY + id;
        template.delete(key);
        return Result.ok();
    }

    public Shop queryWithMutex(Long id)   {
        String lockKey = "lock:shop:" + id;
        String key = CACHE_SHOP_KEY + id;
        String shopJson = null;
        Shop shop = null;
        while (true){
            //从redis中查询数据
            shopJson = template.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }
            if (shopJson != null){
                //log.debug("is null");
                return null;
            }
            /*
                redis中没有数据，开始抢锁操作数据库
                拿到锁成功
            */
            if (lock(lockKey)){
                try{

                    //利用缓存空数据的方式解决缓存穿透，因而会缓存"",isNotBlank会把空串判断为false,如果缓存中不存在，查询数据库
                    shop = getById(id);
                    if (shop == null){
                        //如果数据库中也不存在，将该数据缓存为空数据，防止穿透
                        template.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                        return null;
                    }
                    System.out.println("重构缓存");
                    String  s = JSONUtil.toJsonStr(shop);
                    template.opsForValue().set(key,s,CACHE_SHOP_TTL, TimeUnit.MINUTES);
                    return shop;
                    //最后要释放锁
                }finally {
                    unlock(lockKey);
                }
            }
            //抢锁失败，等待10毫秒继续从redis开始查询
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean lock(String key){
        Boolean flag = template.opsForValue().setIfAbsent(key, "1", 30, TimeUnit.SECONDS);
        //防止自动拆箱返回null
        return BooleanUtil.isTrue(flag);
    }

    public void unlock(String key){
        template.delete(key);
    }

    /*public Shop queryWithMutex(Long id)   {
        String key = CACHE_SHOP_KEY + id;
        String shopJson = template.opsForValue().get(key);
        //log.debug(shopJson);
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //进而会继续走下面的逻辑查数据库，因此需要判断非空
        if (shopJson != null){
            //log.debug("is null");
            return null;
        }
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = lock(lockKey);
            // 获取锁失败
            if(!isLock){
                //4.3 失败，则休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //拿到锁成功，
            //利用缓存空数据的方式解决缓存穿透，因而会缓存"",isNotBlank会把空串判断为false,如果缓存中不存在，查询数据库
            shop = getById(id);
            if (shop == null){
                //如果数据库中也不存在，将该数据缓存为空数据，防止穿透
                template.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            String  s = JSONUtil.toJsonStr(shop);
            System.out.println(s);
            System.out.println("缓存商铺");
            template.opsForValue().set(key,s,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }
        finally {
            unlock(lockKey);
        }
        return shop;
    }*/
}
