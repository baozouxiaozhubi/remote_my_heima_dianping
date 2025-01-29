package com.hsj.hmdp.controller;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.ShopType;
import com.hsj.hmdp.service.IShopTypeService;
import com.hsj.hmdp.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @PostMapping("/seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
