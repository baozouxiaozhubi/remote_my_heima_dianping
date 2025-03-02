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
