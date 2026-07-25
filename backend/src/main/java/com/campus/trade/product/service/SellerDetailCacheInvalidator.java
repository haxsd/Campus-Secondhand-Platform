package com.campus.trade.product.service;

import com.campus.trade.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 卖家信用摘要变化时失效该卖家商品详情缓存的公共服务。
 *
 * <p>商品详情中嵌入了卖家成交数、评分等信用字段，因此评价或纠纷裁决更新信用数据后，
 * 不能只更新 MySQL；必须同时删除相关详情缓存。删除动作延后至事务提交成功之后执行。</p>
 */
@Service
public class SellerDetailCacheInvalidator {
    private final ProductMapper productMapper;
    private final ProductDetailCacheService cacheService;

    public SellerDetailCacheInvalidator(ProductMapper productMapper, ProductDetailCacheService cacheService) {
        this.productMapper = productMapper;
        this.cacheService = cacheService;
    }

    public void invalidateAfterCommit(Long sellerId) {
        Runnable action = () -> productMapper.selectBySeller(sellerId, null)
                .forEach(product -> cacheService.invalidate(product.getId()));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { action.run(); }
        });
    }
}
