package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsj.hmdp.pojo.Voucher;
import com.hsj.hmdp.dto.Result;

public interface IVoucherService extends IService<Voucher> {

    //根据店铺id查询代金券
    Result queryVoucherOfShop(Long shopId);

    //设置某个代金券为秒杀券
    void addSeckillVoucher(Voucher voucher);
}
