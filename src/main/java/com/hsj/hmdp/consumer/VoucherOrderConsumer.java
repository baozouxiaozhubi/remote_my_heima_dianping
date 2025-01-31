package com.hsj.hmdp.consumer;
import apache.rocketmq.v2.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsj.hmdp.dao.VoucherOrderMapper;
import com.hsj.hmdp.dto.Result;
import com.hsj.hmdp.pojo.VoucherOrder;
import com.hsj.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Slf4j
@RocketMQMessageListener(topic = "create_voucher_order", consumerGroup = "consumer-group",consumeThreadNumber = 1)
public class VoucherOrderConsumer implements RocketMQListener<VoucherOrder> {

    public VoucherOrderConsumer() {
        System.out.println("初始化消费者");
    }
    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Transactional //注册为事务，因为RocketMQ的重试机制在异步消息被消费失败时会自动重试，但不会回滚事务，因此需要手动注释
    public void onMessage(VoucherOrder voucherOrder) {
        try {
            // 事务处理订单
            voucherOrderMapper.insert(voucherOrder);
            int flag = voucherOrderMapper.stock_minus(voucherOrder.getVoucherId());
            if (flag == 0) {
                throw new RuntimeException("库存不足");
            }
            System.out.println("成功处理订单: " + voucherOrder.getId());
        } catch (Exception e) {
            log.error("处理订单异常，消息将进行重试", e);
            throw new RuntimeException("消费失败，触发 RocketMQ 重试"); // 让 RocketMQ 进行重试
        }
    }

}
