package com.campus.trade.ai.review;

import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.ProductDetailCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class ProductReviewRunServiceTest {
    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void schedulesAiExecutionOnlyAfterTransactionCommit() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductReviewRunMapper runMapper = mock(ProductReviewRunMapper.class);
        ProductReviewProperties properties = new ProductReviewProperties();
        properties.setEnabled(true);
        ProductReviewRuleService ruleService = mock(ProductReviewRuleService.class);
        ProductReviewAgentService agentService = mock(ProductReviewAgentService.class);
        ProductDetailCacheService cacheService = mock(ProductDetailCacheService.class);
        Executor executor = mock(Executor.class);
        ProductReviewPersistenceService persistence = mock(ProductReviewPersistenceService.class);
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(2L);
        product.setStatus(0);
        product.setStock(1);
        product.setVersion(7);
        when(productMapper.selectById(1L)).thenReturn(Optional.of(product));
        when(productMapper.submitAiReviewBySeller(1L, 2L)).thenReturn(1);
        when(productMapper.selectVersion(1L)).thenReturn(8);
        when(productMapper.selectImageUrlsByProductId(1L)).thenReturn(List.of());
        when(ruleService.currentVersion()).thenReturn("2026-01");
        when(runMapper.insert(any())).thenReturn(1);

        ProductReviewRunService service = new ProductReviewRunService(
                productMapper, runMapper, properties, ruleService, agentService,
                cacheService, new ObjectMapper(), executor, persistence
        );

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        ProductReviewSubmitVO response = service.submit(1L, 2L);

        ArgumentCaptor<ProductReviewRunEntity> captor = ArgumentCaptor.forClass(ProductReviewRunEntity.class);
        verify(runMapper).insert(captor.capture());
        assertEquals(ProductReviewRunStatus.PENDING.name(), captor.getValue().getStatus());
        verify(executor, never()).execute(any());
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(executor).execute(any());
        verifyNoInteractions(agentService);
        assertEquals(6, response.status());
    }

    @Test
    void disabledReviewReleasesProductStuckInAiReviewing() {
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductReviewRunMapper runMapper = mock(ProductReviewRunMapper.class);
        ProductReviewProperties properties = new ProductReviewProperties();
        properties.setEnabled(false);
        ProductReviewRuleService ruleService = mock(ProductReviewRuleService.class);
        ProductReviewAgentService agentService = mock(ProductReviewAgentService.class);
        ProductDetailCacheService cacheService = mock(ProductDetailCacheService.class);
        Executor executor = mock(Executor.class);
        ProductReviewPersistenceService persistence = mock(ProductReviewPersistenceService.class);
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(2L);
        product.setStatus(6);
        product.setStock(1);
        product.setVersion(8);
        when(productMapper.selectById(1L)).thenReturn(Optional.of(product));
        when(productMapper.disableAiReviewBySeller(1L, 2L)).thenReturn(1);
        when(productMapper.selectVersion(1L)).thenReturn(9);

        ProductReviewRunService service = new ProductReviewRunService(
                productMapper, runMapper, properties, ruleService, agentService,
                cacheService, new ObjectMapper(), executor, persistence
        );

        ProductReviewSubmitVO response = service.submit(1L, 2L);

        assertEquals(1, response.status());
        verify(productMapper).disableAiReviewBySeller(1L, 2L);
        verifyNoInteractions(agentService, executor);
    }

}
