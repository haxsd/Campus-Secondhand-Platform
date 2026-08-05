package com.campus.trade.order.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * RocketMQ 订单超时消息配置。
 *
 * <p>这里只保留当前功能真正需要的连接和消息参数。客户端超时、TLS、重试次数等参数
 * 暂时使用 RocketMQ 默认值，等项目出现明确需求时再开放配置。</p>
 */
@ConfigurationProperties(prefix = "campus.order.timeout-message")
public record OrderTimeoutMessageProperties(
        @DefaultValue("localhost:8081") String endpoints,
        @DefaultValue("campus_trade_order_timeout") String topic,
        @DefaultValue("CONFIRM_TIMEOUT") String tag,
        @DefaultValue("campus_trade_order_timeout_consumer") String consumerGroup
) {
}
