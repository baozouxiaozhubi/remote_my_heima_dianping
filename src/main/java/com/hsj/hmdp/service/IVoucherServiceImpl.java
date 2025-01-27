package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.VoucherMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.SeckillVoucher;
import com.hsj.hmdp.pojo.Voucher;
import com.hsj.hmdp.utils.Constants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class IVoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        //1.查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        //2.返回结果
        return Result.ok(vouchers);
    }

    //保存一个优惠券并设置为秒杀
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher); //效果等于"this.baseMapper.insert(voucher);"
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher); //效果等于"seckillVoucherService.baseMapper.insert(seckillVoucher);"
        // 保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(Constants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }
}
