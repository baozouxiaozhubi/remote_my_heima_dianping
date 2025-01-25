package com.hsj.hmdp.controller;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.ShopType;
import com.hsj.hmdp.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Autowired
    private IShopTypeService typeService;

    @GetMapping("/list")
    public Result queryTypeList() throws JsonProcessingException {
        List<ShopType> typeList = typeService.getTypeList();
        return Result.ok(typeList);
    }
}
