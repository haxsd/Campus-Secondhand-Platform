package com.campus.trade.order.mq;

import com.campus.trade.order.service.OrderTimeoutService;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 订单超时延迟消息消费者。
 *
 * <p>消费者不直接编写订单状态、库存和缓存规则，只负责解析消息并调用
 * {@link OrderTimeoutService}。这样 MQ 与定时扫描始终复用同一套关单逻辑。</p>
 */
@Service
public class OrderTimeoutMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutMessageConsumer.class);

    private final OrderTimeoutService timeoutService;

    public OrderTimeoutMessageConsumer(OrderTimeoutService timeoutService) {
        this.timeoutService = timeoutService;
    }

    /**
     * 同步消费一条消息。
     *
     * <p>数据库临时异常返回 FAILURE，让 RocketMQ 重试；消息结构损坏属于永久错误，
     * 记录完整定位信息后 ACK，避免一条坏消息长期占用重试资源。</p>
     */
    public ConsumeResult consume(MessageView messageView) {
        Long orderId;
        try {
            // 消息体只有订单 ID，直接按 UTF-8 字符串读取，不需要 DTO 和 JSON 反序列化。
            String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString().trim();
            orderId = Long.valueOf(body);
            if (orderId <= 0) {
                throw new NumberFormatException("订单 ID 必须大于 0");
            }
        } catch (NumberFormatException exception) {
            // 消息内容固定后，格式错误不会因重试而恢复，确认消息并记录定位信息即可。
            log.error(
                    "订单超时消息格式不正确，停止重试: messageId={}, deliveryAttempt={}",
                    messageView.getMessageId(),
                    messageView.getDeliveryAttempt(),
                    exception
            );
            return ConsumeResult.SUCCESS;
        }

        try {
            timeoutService.cancelIfExpired(orderId);
            log.info(
                    "订单超时消息消费完成: messageId={}, orderId={}, deliveryAttempt={}",
                    messageView.getMessageId(),
                    orderId,
                    messageView.getDeliveryAttempt()
            );
            return ConsumeResult.SUCCESS;
        } catch (Exception exception) {
            // MySQL、Redis 等临时故障返回失败，RocketMQ 会重新投递；事务本身已经回滚。
            log.error(
                    "订单超时消息消费失败，等待 RocketMQ 重试: messageId={}, deliveryAttempt={}",
                    messageView.getMessageId(),
                    messageView.getDeliveryAttempt(),
                    exception
            );
            return ConsumeResult.FAILURE;
        }
    }
}
