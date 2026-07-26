package com.campus.trade.order.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * RocketMQ 订单超时消息配置。
 *
 * <p>默认关闭 MQ。未部署 RocketMQ 时，现有定时扫描仍会独立完成超时关单；
 * 启用后，延迟消息负责及时触发，定时扫描负责补偿发送失败、丢失或积压。</p>
 */
@ConfigurationProperties(prefix = "campus.order.timeout-message")
public record OrderTimeoutMessageProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("localhost:8081") String endpoints,
        @DefaultValue("campus_trade_order_timeout") String topic,
        @DefaultValue("CONFIRM_TIMEOUT") String tag,
        @DefaultValue("campus_trade_order_timeout_consumer") String consumerGroup,
        @DefaultValue("false") boolean sslEnabled,
        @DefaultValue("3s") Duration requestTimeout,
        @DefaultValue("3") int producerMaxAttempts
) {
}
