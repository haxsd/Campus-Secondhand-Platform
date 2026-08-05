package com.campus.trade.order.mq;

import com.campus.trade.order.service.OrderTimeoutService;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 延迟消息消费者的结果语义测试。
 *
 * <p>业务处理成功应确认消息，临时业务异常应要求 RocketMQ 重试，
 * 永久格式错误则应直接确认，避免无意义地重复消费同一条坏消息。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutMessageConsumerTest {

    @Mock
    private MessageView messageView;

    @Mock
    private OrderTimeoutService timeoutService;

    private OrderTimeoutMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderTimeoutMessageConsumer(timeoutService);
    }

    @Test
    void shouldAcknowledgeValidMessage() {
        stubBody("201");

        ConsumeResult result = consumer.consume(messageView);

        assertThat(result).isEqualTo(ConsumeResult.SUCCESS);
        verify(timeoutService).cancelIfExpired(201L);
    }

    @Test
    void shouldAcknowledgeMalformedMessageWithoutCallingBusinessService() {
        stubBody("不是订单ID");

        ConsumeResult result = consumer.consume(messageView);

        assertThat(result).isEqualTo(ConsumeResult.SUCCESS);
        verify(timeoutService, never()).cancelIfExpired(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRequestRetryWhenBusinessServiceFailsTemporarily() {
        stubBody("202");
        doThrow(new IllegalStateException("模拟数据库临时不可用"))
                .when(timeoutService).cancelIfExpired(202L);

        ConsumeResult result = consumer.consume(messageView);

        assertThat(result).isEqualTo(ConsumeResult.FAILURE);
    }

    /**
     * RocketMQ 的消息体是 ByteBuffer；每个测试都创建新的实例，避免读取位置被前一次消费改变。
     */
    private void stubBody(String body) {
        when(messageView.getBody()).thenReturn(
                ByteBuffer.wrap(body.getBytes(StandardCharsets.UTF_8))
        );
    }
}
