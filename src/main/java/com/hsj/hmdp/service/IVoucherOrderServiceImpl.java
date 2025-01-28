package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.VoucherOrderMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.SeckillVoucher;
import com.hsj.hmdp.pojo.VoucherOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class IVoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.根据提交的Id查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否在有效期内
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }
        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }
        //3.判断库存是否充足
        if(seckillVoucher.getStock()<1)
        {
            return Result.fail("库存不足");
        }
        //4.扣减库存
        int flag = getBaseMapper().stock_minus(voucherId);
        if(flag == 0)
        {
            return Result.fail("库存不足");
        }
        //5.创建订单
        //6.返回订单id
        return null;
    }
}
