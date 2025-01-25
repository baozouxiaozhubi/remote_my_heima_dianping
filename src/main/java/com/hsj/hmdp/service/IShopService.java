package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.Shop;

public interface IShopService extends IService<Shop> {
    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
