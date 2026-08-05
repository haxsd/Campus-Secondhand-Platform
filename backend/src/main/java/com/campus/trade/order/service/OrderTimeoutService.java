package com.campus.trade.order.service;

import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.ProductDetailCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 单笔超时订单的幂等事务处理。
 *
 * <p>RocketMQ 消费者和定时扫描都只传 orderId，并调用本类同一个入口。
 * 数据库条件更新决定谁获得关单资格，确保重复消息或并发扫描不会重复回补库存。</p>
 */
@Service
public class OrderTimeoutService {
    private static final String TIMEOUT_REASON = "卖家未在确认期限内处理订单，系统自动取消";
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final ProductDetailCacheService cacheService;

    public OrderTimeoutService(OrderMapper orderMapper, ProductMapper productMapper, ProductDetailCacheService cacheService) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.cacheService = cacheService;
    }

    /**
     * 仅在订单仍待确认且确认期限已过时取消。
     *
     * <p>先查询是为了得到回补库存所需的商品和数量；最终资格仍由带状态、截止时间条件的
     * UPDATE 决定。影响行数为 0 时不执行任何副作用，因此可以安全重复调用。</p>
     */
    @Transactional
    public void cancelIfExpired(Long orderId) {
        TradeOrder order = orderMapper.selectById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (orderMapper.timeoutCancelBySystem(orderId) == 0) {
            return;
        }

        productMapper.restoreStockForCancelledOrder(order.getProductId(), order.getQuantity());
        TradeOrderLog log = new TradeOrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(OrderStatus.PENDING_CONFIRM.getCode());
        log.setToStatus(OrderStatus.TIMEOUT_CANCELLED.getCode());
        log.setOperatorType(2); // 系统操作不归属具体用户。
        log.setReason(TIMEOUT_REASON);
        orderMapper.insertLog(log);
        invalidateAfterCommit(order.getProductId());
    }

    private void invalidateAfterCommit(Long productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheService.invalidate(productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { cacheService.invalidate(productId); }
        });
    }

}
