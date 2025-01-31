package com.hsj.hmdp.producer;
import com.hsj.hmdp.pojo.VoucherOrder;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

@Service
public class VoucherOrderProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private final String topic = "create_voucher_order";

    //  改为异步发送 VoucherOrder对象
    public void sendVoucherOrderAsync(VoucherOrder voucherOrder) {
        //发送异步消息
        rocketMQTemplate.asyncSend(topic, MessageBuilder.withPayload(voucherOrder).build(), new SendCallback() {

            @Override
            public void onSuccess(SendResult sendResult) {
                System.out.printf("✅ 订单消息发送成功: %s\n", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                System.err.printf("❌ 订单消息发送失败: %s\n", throwable.getMessage());

                // 设置最大重试次数
                int maxRetries = 3;
                int retries = 0;

                // 重试逻辑
                while (retries < maxRetries)
                {
                    try
                    {
                        System.out.println("尝试重新发送第 " + (retries + 1) + " 次消息...");
                        rocketMQTemplate.asyncSend(topic, MessageBuilder.withPayload(voucherOrder).build(), this);
                        break; // 如果成功发送，跳出循环
                    }
                    catch (Exception e)
                    {
                        retries++;
                        if (retries >= maxRetries) System.err.println("重试超过最大次数，发送失败，消息丢失: " + voucherOrder);
                    }
                }
            }
        });
    }
}
