package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hsj.hmdp.pojo.VoucherOrder;
import com.hsj.hmdp.dto.Result;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    //秒杀下单
    Result seckillVoucher(Long voucherId);

    Result createVoucherOrder(Long voucherId);

    //第二版 异步秒杀逻辑的创建订单函数
    void createVoucherOrder(VoucherOrder voucherorder);
}
