package com.hsj.hmdp.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.ShopMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.Shop;
import com.hsj.hmdp.utils.Constants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

@Service
public class IShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //1.根据id从Redis查询是否存在商户缓存--用HashMap存对象会导致难以实现【缓存空值】预防缓存穿透
        Map<Object,Object> shopMap=stringRedisTemplate.opsForHash().entries(Constants.CACHE_SHOP+id);
        //2.存在则直接返回
        if(!shopMap.isEmpty())
        {
            Shop shop = BeanUtil.fillBeanWithMap(shopMap,new Shop(),false);
            return Result.ok(shop);
        }
        //3.否则根据id查询数据库
        Shop shop = this.baseMapper.selectById(id);
        //4.数据库不存在则返回错误
        if(shop==null)
        {
            return Result.fail("店铺不存在");
        }
        //6.数据库存在则把Shop对象保存在Redis中
        Map<String,String> shopMap_Str = new HashMap<>();
        Map<String,Object> shopMap_Obj=BeanUtil.beanToMap(shop);
        shopMap_Obj.forEach((k,v)->shopMap_Str.put(k,String.valueOf(v)));
        stringRedisTemplate.opsForHash().putAll(Constants.CACHE_SHOP+id,shopMap_Str);
        //7.设置Shop对象有效期
        stringRedisTemplate.expire(Constants.CACHE_SHOP+id,Constants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //8.返回Shop对象
        return Result.ok(shop);
    }

    @Override
    public Result queryWithMutex(Long id)
    {
        //1.根据id从Redis查询是否存在商户缓存
        Map<Object,Object> shopMap=stringRedisTemplate
                                    .opsForHash()
                                    .entries(Constants.CACHE_SHOP+id);
        //2.判断缓存中是否存在
        if(!shopMap.isEmpty())
        {
            //3.存在则判断是否为占位符
            if(shopMap.containsKey(Constants.EMPTY_KEY))
            {
                //4.存在占位符，直接返回错误信息
                return Result.fail("缓存穿透");
            }
            //5.存在业务数据，直接返回
            return Result.ok(BeanUtil.fillBeanWithMap(shopMap,new Shop(),false));
        }
        //6.实现缓存重建
        Shop shop = null; //为了返回值不报错
        //6.1 获取互斥锁
        String lockKey = Constants.REDIS_LOCK + id;
        try{
            boolean isLock = tryGetLock(lockKey);
            //6.2 判断是否获取成功
            if (!isLock) {
                //6.3 失败，则休眠一段时间并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //6.4 成功，开始查询数据库
            shop = this.baseMapper.selectById(id);
            //7.数据库中不存在对应id则返回错误，同时在缓存中存入占位符(缓存空值法)
            if (shop == null) {
                stringRedisTemplate.opsForHash().put(Constants.CACHE_SHOP + id, Constants.EMPTY_KEY, Constants.EMPTY_VALUE);
                stringRedisTemplate.expire(Constants.CACHE_SHOP + id, Constants.CACHE_SHOP_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("店铺不存在");
            }
            //8.数据库中存在对应id则把Shop对象保存在Redis中并设置有效期
            Map<String, String> shopMap_Str = new HashMap<>();
            Map<String, Object> shopMap_obj = BeanUtil.beanToMap(shop);
            shopMap_obj.forEach((k, v) -> shopMap_Str.put(k, String.valueOf(v)));
            stringRedisTemplate.opsForHash().putAll(Constants.CACHE_SHOP + id, shopMap_Str);
            stringRedisTemplate.expire(Constants.CACHE_SHOP + id, Constants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
        catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            //9.释放互斥锁防止死锁
            unlock(lockKey);
        }
        return Result.ok(shop);
    }

    @Override
    public Result update(Shop shop) {
        //缓存更新策略：Cache Aside Pattern 先更再删
        //1.更新数据库
        this.baseMapper.updateById(shop);
        //2.获取shop的id(确保非空)
        Long id = shop.getId();
        if(id==null)
        {
            return Result.fail("店铺id不能为空");
        }
        //3.删除缓存
        stringRedisTemplate.delete(Constants.CACHE_SHOP+id);
        //4.返回结果
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        return null;
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
