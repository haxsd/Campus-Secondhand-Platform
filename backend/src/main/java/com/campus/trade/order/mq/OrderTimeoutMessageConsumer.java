package com.campus.trade.order.mq;

import com.campus.trade.order.service.OrderTimeoutService;
import com.campus.trade.order.service.OrderTimeoutService.CancelResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 订单超时延迟消息消费者。
 *
 * <p>消费者不直接编写订单状态、库存和缓存规则，只负责解析消息并调用
 * {@link OrderTimeoutService}。这样 MQ 与定时扫描始终复用同一套关单逻辑。</p>
 */
@Service
public class OrderTimeoutMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutMessageConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderTimeoutService timeoutService;

    public OrderTimeoutMessageConsumer(ObjectMapper objectMapper, OrderTimeoutService timeoutService) {
        this.objectMapper = objectMapper;
        this.timeoutService = timeoutService;
    }

    /**
     * 同步消费一条消息。
     *
     * <p>数据库临时异常返回 FAILURE，让 RocketMQ 重试；消息结构损坏属于永久错误，
     * 记录完整定位信息后 ACK，避免一条坏消息长期占用重试资源。</p>
     */
    public ConsumeResult consume(MessageView messageView) {
        try {
            OrderTimeoutEvent event = readEvent(messageView.getBody());
            event.validate();

            CancelResult result = timeoutService.cancelIfExpired(event.orderId());
            log.info(
                    "订单超时消息消费完成: messageId={}, eventId={}, orderId={}, result={}, deliveryAttempt={}",
                    messageView.getMessageId(),
                    event.eventId(),
                    event.orderId(),
                    result,
                    messageView.getDeliveryAttempt()
            );
            return ConsumeResult.SUCCESS;
        } catch (IOException | IllegalArgumentException exception) {
            // 永久格式错误即使重试也不会恢复，ACK 后依靠日志和定时扫描保障业务订单。
            log.error(
                    "订单超时消息格式不正确，停止重试: messageId={}, deliveryAttempt={}",
                    messageView.getMessageId(),
                    messageView.getDeliveryAttempt(),
                    exception
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

    private OrderTimeoutEvent readEvent(ByteBuffer body) throws IOException {
        byte[] bytes = new byte[body.remaining()];
        body.get(bytes);
        return objectMapper.readValue(bytes, OrderTimeoutEvent.class);
    }
}
