package com.campus.trade.order.service;

import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.ProductDetailCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 单笔超时订单的事务处理，单独成 Bean 以确保定时任务调用时 @Transactional 生效。 */
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

    /** 仅在订单仍待确认且确认期限已过时取消；条件更新防止与卖家确认请求发生竞态。 */
    @Transactional
    public void cancelIfExpired(TradeOrder order) {
        if (orderMapper.timeoutCancelBySystem(order.getId()) == 0) {
            return;
        }
        productMapper.restoreStockForCancelledOrder(order.getProductId(), order.getQuantity());
        TradeOrderLog log = new TradeOrderLog();
        log.setOrderId(order.getId());
        log.setFromStatus(0);
        log.setToStatus(4);
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
