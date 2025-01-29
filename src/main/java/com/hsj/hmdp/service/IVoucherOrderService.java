package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsj.hmdp.pojo.VoucherOrder;
import com.hsj.hmdp.dto.Result;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    //秒杀下单
    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);
}
