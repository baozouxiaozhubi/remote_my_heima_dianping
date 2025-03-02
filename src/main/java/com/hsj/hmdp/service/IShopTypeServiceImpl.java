package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hsj.hmdp.dao.ShopTypeMapper;
import com.hsj.hmdp.pojo.ShopType;
import com.hsj.hmdp.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//添加Redis缓存
@Service
public class IShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public List<ShopType> getTypeList() throws JsonProcessingException {
        //1.查询Redis中是否存在ShopeType缓存
        List<ShopType> shopTypeList = getShopTypeList(Constants.CACHE_SHOP_TYPE);
        //2.判断缓存是否存在
        if(!shopTypeList.isEmpty())
        {
            //3.存在则直接返回
            return shopTypeList;
        }
        //4.否则从数据库中获取
        shopTypeList=this.baseMapper.selectList(new QueryWrapper<>());
        //5.将ShopType列表保存在Redis中
        saveShopTypeList(Constants.CACHE_SHOP_TYPE,shopTypeList);
        //6.设置缓存有效期
        stringRedisTemplate.expire(Constants.CACHE_SHOP_TYPE,Constants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        //7.返回ShopType列表
        return shopTypeList;
    }

    //把ShopType对象列表存进Redis缓存中
    public void saveShopTypeList(String key, List<ShopType> shopTypeList) throws JsonProcessingException
    {
        // 将 List 中的用户对象序列化为 JSON 字符串
        for (ShopType shopType : shopTypeList) {
            String userJson = objectMapper.writeValueAsString(shopType);
            // 存储到 Redis
            stringRedisTemplate.opsForList().rightPush(key, userJson);
        }
    }

    //从Redis缓存中读取ShopType对象列表
    public List<ShopType> getShopTypeList(String key) throws JsonProcessingException
    {
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(key, 0, -1); // 获取所有店铺JSON 字符串
        return shopTypeJsonList.stream()
                .map(shopTypeJson -> {
                    try {
                        return objectMapper.readValue(shopTypeJson, ShopType.class); // 反序列化 JSON
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return null; // 处理反序列化异常
                    }
                })
                .collect(Collectors.toList());
    }

}
