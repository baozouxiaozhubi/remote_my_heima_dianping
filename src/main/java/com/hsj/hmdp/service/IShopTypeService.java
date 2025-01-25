package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hsj.hmdp.pojo.ShopType;

import java.util.List;

public interface IShopTypeService extends IService<ShopType> {
    List<ShopType> getTypeList() throws JsonProcessingException;
}
