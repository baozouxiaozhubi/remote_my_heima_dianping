package com.hsj.hmdp.controller;

import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.service.IShopService;
import org.springframework.web.bind.annotation.*;
import com.hsj.hmdp.pojo.Shop;
import javax.annotation.Resource;

@RestController
@RequestMapping("/shop")
public class ShopController {
    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    //Restful风格传参
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryWithMutex(id);
        //return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库-因为shopService继承自ServiceImpl<ShopMapper, Shop> 所以自带save()方法，可以传入Shop对象，保存到数据库中
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库
        return shopService.update(shop);
    }


}
