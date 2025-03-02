package com.hsj.hmdp.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hsj.hmdp.consumer.VoucherOrderConsumer;
import com.hsj.hmdp.dao.VoucherOrderMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.VoucherOrder;
import com.hsj.hmdp.producer.VoucherOrderProducer;
import com.hsj.hmdp.utils.*;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private VoucherOrderProducer voucherOrderProducer; //第二种方案：使用RocketMQ消息队列
    //只需要使用@RocketMQMessageListener注释注册消费者，Spring就会自动监听并阻塞等待，

    private IVoucherOrderService proxy; //事务代理对象 由于只有主线程能获取事务代理对象，然而子线程中要调用，因此把作用域提前
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);//初始化阻塞队列，最多1024个订单等待创建
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();//用单线程线程池，因为Redis已经实现了订单创建功能，数据库就没有性能要求了

    @PostConstruct //@PostConstruct注解，会在整个Bean构建并完成依赖注入后执行方法
    private void init() {
        //SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    //用于从阻塞队列/消息队列中获取订单信息并创建订单
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while(true)
            {
                try{
                    //第一种方案从阻塞队列中获取订单信息
                    VoucherOrder voucherOrder = orderTasks.take();

                    //第二种方案：从RocketMQ中获取订单信息(放在消费者类中写了)

                    //创建订单
                    //由于在Redis中已经做了秒杀资格判断和库存判断了，所以其实这里不用加锁
                    proxy.createVoucherOrder(voucherOrder);
                }
                catch (Exception e){
                    log.error("处理订单异常",e);
                }
            }
        }
    }

    //第二版 异步秒杀逻辑的创建订单函数--BlockingQueue实现
    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherorder) {
        save(voucherorder);
    }

    //Lua脚本预先读取
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static
    {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setResultType(Long.class);
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));

    }

    //第二版 异步秒杀逻辑的下单函数--包含BlockingQueue实现和消息队列实现
    @Override
    public Result seckillVoucher(Long voucherId) {
        //0. 获取用户Id
        Long userId = UserContext.getUser().getId();
        //1. 执行Lua脚本--也可以加分布式锁
        Long r = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId,userId
                );
        //2. 判断结果是否为0
        if(r!=0)
        {
            //3. 不为0代表没有购买资格，直接返回错误信息
            return Result.fail(r==1?"库存不足":"请勿重复下单");
        }
        //4. 为0，代表有购买资格，把用户信息保存到阻塞队列中
        //TODO：保存到阻塞队列中
        //新建一个VoucherOrder对象用于保存用户信息
        VoucherOrder newVoucherOrder = new VoucherOrder();
        long orderId=redisIdWorker.nextId(Constants.ID_PREFIX_ORDER);
        newVoucherOrder.setId(orderId);
        //设置用户Id
        newVoucherOrder.setUserId(userId);
        //获取代金券Id
        newVoucherOrder.setVoucherId(voucherId);

        //首先初始化事务代理对象(只有主线程能获取)-放入消费者类
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        //第一种方案：放入阻塞队列(BlockingQue实现)
        //orderTasks.add(newVoucherOrder);

        //第二种方案：放入RocketMQ
        voucherOrderProducer.sendVoucherOrderAsync(newVoucherOrder);

        //5. 返回订单Id
        return Result.ok(orderId);
    }

    @Override
    public Result createVoucherOrder(Long voucherId) {
        return null;
    }

    /*** 第一版 同步秒杀逻辑
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

        //4.一人(同一时间)一单
        Long userId = UserContext.getUser().getId();

        //使用synchronized代码块加锁--只在单机模式下管用
//        synchronized (userId.toString().intern()) {
//            //7.返回订单id
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }

        //使用Redis加分布式锁--在分布式下也管用
        //ILock lock = new SimpleRedisLock(stringRedisTemplate,"order:"+userId);

        //使用开源框架Redisson获取可重入的，可重试的，主从一致的分布式锁
        RLock lock = redissonClient.getLock("order:"+userId);
        boolean isLock = lock.tryLock();  //三个参数 重试等待时间，过期时间，过期时间单位

        if(!isLock)
        {
            return Result.fail("请不要重复下单");
        }
        try {
            proxy = (IVoucherOrderService) AopContext.currentProxy();
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
    ***/

    /***第一版 同步下单的创建订单函数--判断用户购买资格+防止超卖都在这里处理
    //保证【判断资格】和【创建订单】的原子性
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

        //乐观锁--版本号法
//        boolean success = seckillVoucherService.update()
//                .setSql("stock = stock - 1")
//                .eq("voucher_id",voucherId)
//                .eq("stock",seckillVoucher.getStock())
//                .update();
//        if(!success)return Result.fail("库存不足");

        //乐观锁改进=1 只在修改时候判断stock>0
        int stock = getBaseMapper().select_Stock_For_Update(voucherId);
        if (stock <= 0) {
            return Result.fail("库存不足");
        }
        getBaseMapper().stock_minus(voucherId); // 实际扣减库存操作

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
    }//@Transactional注释加在方法上 结束之后还需要提交事务 先释放锁在提交事务，在这段时间中还可能出现线程安全问题--所以应该加在方法外，先提交事务再释放锁
    ***/
}
