package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.dao.VoucherOrderMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.SeckillVoucher;
import com.hsj.hmdp.pojo.VoucherOrder;
import com.hsj.hmdp.utils.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class IVoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

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

        //4.一人一单
        Long userId = UserContext.getUser().getId();

        //使用synchronized代码块加锁--只在单机模式下管用
//        synchronized (userId.toString().intern()) {
//            //7.返回订单id
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }

        //使用Redis加分布式锁
        //ILock lock = new SimpleRedisLock(stringRedisTemplate,"order:"+userId);

        //使用开源框架Redisson获取可重入的，可重试的，主从一致的分布式锁
        RLock lock = redissonClient.getLock("order:"+userId);
        boolean isLock = lock.tryLock();  //三个参数 重试等待时间，过期时间，过期时间单位

        if(!isLock)
        {
            return Result.fail("请不要重复下单");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId)
    {
        Long userId = UserContext.getUser().getId();
        //4.1 查询当前用户是否购买过查询的优惠券
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //4.2 已经购买过则报错
        if (count > 0) {
            return Result.fail("已经购买过优惠券了");
        }

        //5.未购买过则扣减库存--需要避免并发问题(1.在SQL语句加上where stock>0 2.乐观锁-版本号法 3.乐观锁-CAS)
        //乐观锁
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock - 1")
//                .eq("voucher_id",voucherId)
//                .eq("stock",seckillVoucher.getStock())
//                .update();
//        if(!success)return Result.fail("库存不足");
        //乐观锁改进 只在修改时候判断stock>0
        int flag = getBaseMapper().stock_minus(voucherId);
        if (flag == 0) {
            return Result.fail("库存不足");
        }

        //6.创建订单
        VoucherOrder newVoucherOrder = new VoucherOrder();
        //6.1 获取全局唯一订单id
        long orderId = redisIdWorker.nextId(Constants.ID_PREFIX_ORDER);
        newVoucherOrder.setId(orderId);
        //6.2 设置用户Id
        newVoucherOrder.setUserId(userId);
        //6.3 获取代金券Id
        newVoucherOrder.setVoucherId(voucherId);
        save(newVoucherOrder);
        return Result.ok(orderId);
    }//@Transactional注释加在方法上 结束之后还需要提交事务
    //先释放锁在提交事务，在这段时间中还可能出现线程安全问题--所以应该加在方法外，先提交事务再释放锁
}
