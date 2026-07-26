package com.campus.trade.order.service;

import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.ProductDetailCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超时关单幂等规则测试。
 *
 * <p>MQ 重复投递、定时任务重复扫描以及卖家确认并发到达时，都可能多次调用同一订单；
 * 本测试验证只有数据库条件更新成功的一次调用可以回补库存。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutServiceTest {

    private static final long ORDER_ID = 201L;
    private static final long PRODUCT_ID = 101L;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductDetailCacheService cacheService;

    private OrderTimeoutService timeoutService;

    @BeforeEach
    void setUp() {
        timeoutService = new OrderTimeoutService(orderMapper, productMapper, cacheService);
    }

    @Test
    void shouldCancelAndRestoreStockWhenWinningStatusTransition() {
        TradeOrder order = pendingOrder();
        when(orderMapper.selectById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.timeoutCancelBySystem(ORDER_ID)).thenReturn(1);

        OrderTimeoutService.CancelResult result = timeoutService.cancelIfExpired(ORDER_ID);

        assertThat(result).isEqualTo(OrderTimeoutService.CancelResult.CANCELLED);
        verify(productMapper).restoreStockForCancelledOrder(PRODUCT_ID, 2);
        ArgumentCaptor<TradeOrderLog> logCaptor = ArgumentCaptor.forClass(TradeOrderLog.class);
        verify(orderMapper).insertLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING_CONFIRM.getCode());
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.TIMEOUT_CANCELLED.getCode());
        verify(cacheService).invalidate(PRODUCT_ID);
    }

    @Test
    void shouldNotRestoreStockWhenOrderWasAlreadyHandled() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(Optional.of(pendingOrder()));
        when(orderMapper.timeoutCancelBySystem(ORDER_ID)).thenReturn(0);

        OrderTimeoutService.CancelResult result = timeoutService.cancelIfExpired(ORDER_ID);

        assertThat(result).isEqualTo(OrderTimeoutService.CancelResult.SKIPPED);
        verify(productMapper, never()).restoreStockForCancelledOrder(any(), any());
        verify(orderMapper, never()).insertLog(any());
        verify(cacheService, never()).invalidate(any());
    }

    private TradeOrder pendingOrder() {
        TradeOrder order = new TradeOrder();
        order.setId(ORDER_ID);
        order.setProductId(PRODUCT_ID);
        order.setQuantity(2);
        order.setStatus(OrderStatus.PENDING_CONFIRM.getCode());
        return order;
    }
}
