package com.hsj.hmdp.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.ShopMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.Shop;
import com.hsj.hmdp.utils.CacheClient;
import com.hsj.hmdp.utils.Constants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;

@Service
public class IShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

//    public Result queryById(Long id) {
//        //1.根据id从Redis查询是否存在商户缓存--用HashMap存对象会导致难以实现【缓存空值】预防缓存穿透
//        Map<Object,Object> shopMap=stringRedisTemplate.opsForHash().entries(Constants.CACHE_SHOP+id);
//        //2.存在则直接返回
//        if(!shopMap.isEmpty())
//        {
//            Shop shop = BeanUtil.fillBeanWithMap(shopMap,new Shop(),false);
//            return Result.ok(shop);
//        }
//        //3.否则根据id查询数据库
//        Shop shop = this.baseMapper.selectById(id);
//        //4.数据库不存在则返回错误
//        if(shop==null)
//        {
//            return Result.fail("店铺不存在");
//        }
//        //6.数据库存在则把Shop对象保存在Redis中
//        Map<String,String> shopMap_Str = new HashMap<>();
//        Map<String,Object> shopMap_Obj=BeanUtil.beanToMap(shop);
//        shopMap_Obj.forEach((k,v)->shopMap_Str.put(k,String.valueOf(v)));
//        stringRedisTemplate.opsForHash().putAll(Constants.CACHE_SHOP+id,shopMap_Str);
//        //7.设置Shop对象有效期
//        stringRedisTemplate.expire(Constants.CACHE_SHOP+id,Constants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        //8.返回Shop对象
//        return Result.ok(shop);
//    }

//    public Result queryWithMutex(Long id)
//    {
//        //1.根据id从Redis查询是否存在商户缓存
//        Map<Object,Object> shopMap=stringRedisTemplate
//                                    .opsForHash()
//                                    .entries(Constants.CACHE_SHOP+id);
//        //2.判断缓存中是否存在
//        if(!shopMap.isEmpty())
//        {
//            //3.存在则判断是否为占位符
//            if(shopMap.containsKey(Constants.EMPTY_KEY))
//            {
//                //4.存在占位符，直接返回错误信息
//                return Result.fail("缓存穿透");
//            }
//            //5.存在业务数据，直接返回
//            return Result.ok(BeanUtil.fillBeanWithMap(shopMap,new Shop(),false));
//        }
//        //6.实现缓存重建
//        Shop shop = null; //为了返回值不报错
//        //6.1 获取互斥锁
//        String lockKey = Constants.REDIS_LOCK + id;
//        try{
//            boolean isLock = cacheClient.tryGetLock(lockKey);
//            //6.2 判断是否获取成功
//            if (!isLock) {
//                //6.3 失败，则休眠一段时间并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //6.4 成功，开始查询数据库
//            shop = this.baseMapper.selectById(id);
//            //7.数据库中不存在对应id则返回错误，同时在缓存中存入占位符(缓存空值法)
//            if (shop == null) {
//                stringRedisTemplate.opsForHash().put(Constants.CACHE_SHOP + id, Constants.EMPTY_KEY, Constants.EMPTY_VALUE);
//                stringRedisTemplate.expire(Constants.CACHE_SHOP + id, Constants.CACHE_SHOP_NULL_TTL, TimeUnit.MINUTES);
//                return Result.fail("店铺不存在");
//            }
//            //8.数据库中存在对应id则把Shop对象保存在Redis中并设置有效期
//            Map<String, String> shopMap_Str = new HashMap<>();
//            Map<String, Object> shopMap_obj = BeanUtil.beanToMap(shop);
//            shopMap_obj.forEach((k, v) -> shopMap_Str.put(k, String.valueOf(v)));
//            stringRedisTemplate.opsForHash().putAll(Constants.CACHE_SHOP + id, shopMap_Str);
//            stringRedisTemplate.expire(Constants.CACHE_SHOP + id, Constants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        }
//        catch(InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        finally {
//            //9.释放互斥锁防止死锁
//            cacheClient.unlock(lockKey);
//        }
//        return Result.ok(shop);
//    }

    //利用缓存空值的方式解决缓存穿透 采用JSON格式存储对象
    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient.queryWithTTL(Constants.CACHE_SHOP, id, Shop.class,
                this.baseMapper::selectById, Constants.CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    public Result queryWithLogicalExpireById(Long id) {
        Shop shop = cacheClient.queryWithLogicalTTL(Constants.CACHE_SHOP, id, Shop.class,
                this.baseMapper::selectById, Constants.CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    public Result queryWithMutexById(Long id) {
        Shop shop = cacheClient.queryWithMutex(Constants.CACHE_SHOP, id, Shop.class,
                this.baseMapper::selectById, Constants.CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional //注释为事务，报错直接回滚
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
        //1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, Constants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //2.计算分页参数
        int from = (current-1) * Constants.DEFAULT_PAGE_SIZE;
        int end = current * Constants.DEFAULT_PAGE_SIZE;
        //3.查询Redis，并按距离排序 GEOSEARCH BYLONLAT x y BYRADIUS DIST 单位 WITHDISTANCE
        String key = Constants.SHOP_GEO_KEY + typeId;
        System.out.println(key);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key,
                        GeoReference.fromCoordinate(new Point(x, y)),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        //4.解析出id
        if(results == null)return Result.ok(Collections.emptyList());
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size()<=from)return Result.ok(Collections.emptyList());
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        //4.1 截取出from到end的部分
        list.stream().skip(from).forEach(result -> {
            //4.2 获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //4.3 获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        //5.根据id查询Shop
        String idStr = StrUtil.join(",",ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD( id, " + idStr + ")").list();
        shops.forEach(shop -> {shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());});
        //6.返回
        return Result.ok(shops);
    }



}
