package com.hsj.hmdp;

import com.hsj.hmdp.pojo.Shop;
import com.hsj.hmdp.service.IShopService;
import com.hsj.hmdp.utils.Constants;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
class HmdpApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IShopService shopService;

    //把商户信息缓存到Redis的GEO集合中(按类型分组)
    @Test
    void loadShopData() {
        //1.查询所有店铺信息
        List<Shop> list = shopService.list();

        //2.把店铺按照typeId分组 得到一个键为typeid,值为属于这个类型的商户列表的Map对象
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));

       //3.分批完成写入Redis
        for(Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //3.1 获取类型id
            Long shopId = entry.getKey();
            String key = Constants.SHOP_GEO_KEY + shopId;
            //3.2 获取店铺集合
            List<Shop> shops = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shops.size());
            //3.3 写入Redis的GEO集合
            for (Shop shop : shops) {
                //stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString()); //每一个点建立一次请求，效率低
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY()))
                );//统一写入
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
            stringRedisTemplate.expire(key,100, TimeUnit.DAYS);
        }
    }

}
