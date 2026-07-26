package com.campus.trade.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

/**
 * 订单超时延迟消息生产者。
 *
 * <p>该类始终可以被订单 Service 注入；MQ 开关关闭时 Producer Bean 不存在，
 * publish 会直接返回，因此不会破坏原有定时扫描功能。</p>
 */
@Service
public class OrderTimeoutMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutMessagePublisher.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectProvider<Producer> producerProvider;
    private final ObjectProvider<ClientServiceProvider> clientServiceProvider;
    private final OrderTimeoutMessageProperties properties;
    private final ObjectMapper objectMapper;

    public OrderTimeoutMessagePublisher(
            ObjectProvider<Producer> producerProvider,
            ObjectProvider<ClientServiceProvider> clientServiceProvider,
            OrderTimeoutMessageProperties properties,
            ObjectMapper objectMapper
    ) {
        this.producerProvider = producerProvider;
        this.clientServiceProvider = clientServiceProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送一条在订单确认截止时间才可消费的消息。
     *
     * <p>发送失败只记录错误，不向已经提交成功的下单接口抛异常；
     * 数据库中的 confirm_deadline 和定时扫描会继续保证最终关单。</p>
    */
    public void publish(OrderTimeoutEvent event) {
        try {
            Producer producer = producerProvider.getIfAvailable();
            ClientServiceProvider provider = clientServiceProvider.getIfAvailable();
            if (producer == null || provider == null) {
                // MQ 未启用时不打印错误；这是本地开发和降级运行的正常状态。
                return;
            }

            event.validate();
            long deliveryTimestamp = event.confirmDeadline()
                    .atZone(BUSINESS_ZONE)
                    .toInstant()
                    .toEpochMilli();
            Message message = provider.newMessageBuilder()
                    .setTopic(properties.topic())
                    .setTag(properties.tag())
                    .setKeys(event.eventId(), "order-" + event.orderId())
                    .setDeliveryTimestamp(deliveryTimestamp)
                    .setBody(objectMapper.writeValueAsBytes(event))
                    .build();

            // 异步发送让下单响应不等待 MQ 网络结果；最终失败仍由 confirm_deadline 扫描补偿。
            producer.sendAsync(message).whenComplete((receipt, throwable) -> {
                if (throwable != null) {
                    log.error(
                            "订单超时延迟消息发送失败，将由定时任务兜底: eventId={}, orderId={}",
                            event.eventId(),
                            event.orderId(),
                            throwable
                    );
                    return;
                }
                log.info(
                        "订单超时延迟消息发送成功: eventId={}, orderId={}, messageId={}, deliveryTime={}",
                        event.eventId(),
                        event.orderId(),
                        receipt.getMessageId(),
                        event.confirmDeadline()
                );
            });
        } catch (Exception exception) {
            /*
             * 这里连客户端即时抛出的运行时异常也要兜住。该方法在订单事务提交后执行，
             * 此时不能让 MQ 故障把“实际已创建成功”的订单伪装成接口失败。
             */
            log.error(
                    "订单超时延迟消息发送失败，将由定时任务兜底: eventId={}, orderId={}",
                    event == null ? null : event.eventId(),
                    event == null ? null : event.orderId(),
                    exception
            );
        }
    }
}
