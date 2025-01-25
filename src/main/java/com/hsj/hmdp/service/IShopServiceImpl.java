package com.hsj.hmdp.service;

import cn.hutool.core.bean.BeanUtil;
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
        //1.根据id从Redis查询是否存在商户缓存
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
        //5.数据库存在则把Shop对象保存在Redis中
        Map<String,String> shopMap_Str = new HashMap<>();
        Map<String,Object> shopMap_Obj=BeanUtil.beanToMap(shop);
        shopMap_Obj.forEach((k,v)->shopMap_Str.put(k,String.valueOf(v)));
        stringRedisTemplate.opsForHash().putAll(Constants.CACHE_SHOP+id,shopMap_Str);
        //6.设置Shop对象有效期
        stringRedisTemplate.expire(Constants.CACHE_SHOP+id,Constants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //6.返回Shop对象
        return Result.ok(shop);
    }

    @Override
    public Result update(Shop shop) {
        return null;
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        return null;
    }
}
