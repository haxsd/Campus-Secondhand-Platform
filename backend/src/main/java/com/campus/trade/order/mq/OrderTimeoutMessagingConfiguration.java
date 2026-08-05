package com.campus.trade.order.mq;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RocketMQ 订单超时消息客户端配置。
 *
 * <p>只有 {@code campus.order.timeout-message.enabled=true} 时才创建网络客户端。
 * 默认关闭可以保证开发者只启动 MySQL、Redis 时，原有订单功能仍然正常工作。</p>
 */
@Configuration
@EnableConfigurationProperties(OrderTimeoutMessageProperties.class)
public class OrderTimeoutMessagingConfiguration {

    /**
     * RocketMQ 5 客户端统一入口，只是本地工厂对象，不会在这里建立网络连接。
     */
    @Bean
    public ClientServiceProvider orderTimeoutClientServiceProvider() {

        return ClientServiceProvider.loadService();
    }

    /**
     * 客户端连接 RocketMQ Proxy 的公共配置，创建该对象本身也不会连接网络。
     */
    @Bean
    public ClientConfiguration orderTimeoutClientConfiguration(OrderTimeoutMessageProperties properties) {
        return ClientConfiguration.newBuilder()
                .setEndpoints(properties.endpoints())
                .enableSsl(properties.sslEnabled())
                .build();
    }

    /**
     * 单例生产者。没有特殊需求时直接使用 RocketMQ 客户端默认重试配置。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "campus.order.timeout-message",
            name = "enabled",
            havingValue = "true"
    )
    public Producer orderTimeoutProducer(
            ClientServiceProvider provider,
            ClientConfiguration clientConfiguration,
            OrderTimeoutMessageProperties properties
    ) throws ClientException {
        return provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(properties.topic())
                .build();
    }

    /**
     * 单例 PushConsumer。返回 FAILURE 时由 RocketMQ 按消费重试策略重新投递。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "campus.order.timeout-message",
            name = "enabled",
            havingValue = "true"
    )
    public PushConsumer orderTimeoutPushConsumer(
            ClientServiceProvider provider,
            ClientConfiguration clientConfiguration,
            OrderTimeoutMessageProperties properties,
            OrderTimeoutMessageConsumer messageConsumer
    ) throws ClientException {
        FilterExpression filter = new FilterExpression(properties.tag(), FilterExpressionType.TAG);
        return provider.newPushConsumerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setConsumerGroup(properties.consumerGroup())
                .setSubscriptionExpressions(Map.of(properties.topic(), filter))
                .setMessageListener(messageConsumer::consume)
                .build();
    }
}
