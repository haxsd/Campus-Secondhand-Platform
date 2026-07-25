package com.campus.trade.order.service;

import com.campus.trade.order.mapper.OrderMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 批量扫描待卖家确认的超时订单。
 *
 * <p>扫描和单笔处理分开：扫描不加长事务锁，单笔处理使用条件 UPDATE 重新确认资格，
 * 因而多个应用实例同时运行也不会重复回补库存。</p>
 */
@Component
@ConditionalOnProperty(prefix = "campus.order", name = "timeout-scan-enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutScheduler {
    private static final int BATCH_SIZE = 100;
    private final OrderMapper orderMapper;
    private final OrderTimeoutService timeoutService;

    public OrderTimeoutScheduler(OrderMapper orderMapper, OrderTimeoutService timeoutService) {
        this.orderMapper = orderMapper;
        this.timeoutService = timeoutService;
    }

    @Scheduled(fixedDelayString = "${campus.order.timeout-scan-delay-ms:60000}")
    public void cancelExpiredOrders() {
        orderMapper.selectExpiredPendingOrders(BATCH_SIZE).forEach(timeoutService::cancelIfExpired);
    }
}
