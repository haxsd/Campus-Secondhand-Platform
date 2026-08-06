package com.campus.trade.ai.review;

import com.campus.trade.common.exception.BizException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void highConfidenceRejectTargetsRejectedStatus() {
        ProductReviewRunMapper runMapper = mock(ProductReviewRunMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductReviewProperties properties = new ProductReviewProperties();
        properties.setAutoRejectMinConfidence(0.8);
        ProductReviewAgentService agent = mock(ProductReviewAgentService.class);
        ProductReviewPersistenceService persistence = mock(ProductReviewPersistenceService.class);
        ProductReviewRunEntity run = runningRun();
        when(runMapper.selectByRunId("run")).thenReturn(run);
        when(runMapper.markRunning("run")).thenReturn(1);
        when(agent.review(any())).thenReturn(rejectResult(0.95));
        ProductReviewRunService service = service(productMapper, runMapper, properties, agent, persistence);

        service.executeAfterCommit("run");

        verify(persistence).complete(eq(run), any(ProductReviewResult.class), eq(2));
    }

    @Test
    void lowConfidenceRejectTargetsManualReviewStatus() {
        ProductReviewRunMapper runMapper = mock(ProductReviewRunMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductReviewProperties properties = new ProductReviewProperties();
        properties.setAutoRejectMinConfidence(0.8);
        ProductReviewAgentService agent = mock(ProductReviewAgentService.class);
        ProductReviewPersistenceService persistence = mock(ProductReviewPersistenceService.class);
        ProductReviewRunEntity run = runningRun();
        when(runMapper.selectByRunId("run")).thenReturn(run);
        when(runMapper.markRunning("run")).thenReturn(1);
        when(agent.review(any())).thenReturn(rejectResult(0.79));
        ProductReviewRunService service = service(productMapper, runMapper, properties, agent, persistence);

        service.executeAfterCommit("run");

        verify(persistence).complete(eq(run), argThat(result ->
                result.decision() == ProductReviewDecision.NEED_MANUAL_REVIEW), eq(1));
    }

    @Test
    void passTargetsManualReviewStatus() {
        ProductReviewRunMapper runMapper = mock(ProductReviewRunMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductReviewProperties properties = new ProductReviewProperties();
        ProductReviewAgentService agent = mock(ProductReviewAgentService.class);
        ProductReviewPersistenceService persistence = mock(ProductReviewPersistenceService.class);
        ProductReviewRunEntity run = runningRun();
        when(runMapper.selectByRunId("run")).thenReturn(run);
        when(runMapper.markRunning("run")).thenReturn(1);
        when(agent.review(any())).thenReturn(new ProductReviewResult(ProductReviewDecision.PASS,
                ProductReviewRiskLevel.LOW, 0.99, List.of("信息完整"), List.of(), List.of()));
        ProductReviewRunService service = service(productMapper, runMapper, properties, agent, persistence);

        service.executeAfterCommit("run");

        verify(persistence).complete(eq(run), any(ProductReviewResult.class), eq(1));
    }

    @Test
    void versionConflictMarksRunStale() {
        ProductReviewRunMapper runMapper = mock(ProductReviewRunMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductReviewRunEntity run = runningRun();
        ProductReviewPersistenceService persistence = new ProductReviewPersistenceService(
                runMapper, productMapper, new ObjectMapper());
        when(productMapper.completeAiReview(1L, 2, 1)).thenReturn(0);

        assertEquals(false, persistence.complete(run, new ProductReviewResult(ProductReviewDecision.PASS,
                ProductReviewRiskLevel.LOW, 0.9, List.of("完整"), List.of(), List.of()), 1));

        verify(runMapper).markStale("run", "VERSION_OR_STATUS_CHANGED");
        verify(productMapper, never()).insertAiReviewLog(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void sellerCanAppealAiRejectButNotManualReject() {
        ProductMapper productMapper = mock(ProductMapper.class);
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(2L);
        product.setStatus(2);
        when(productMapper.selectById(1L)).thenReturn(Optional.of(product));
        when(productMapper.selectLatestReviewOperatorType(1L)).thenReturn(1);
        when(productMapper.appealAiRejectBySeller(1L, 2L)).thenReturn(1);
        ProductReviewRunService service = service(productMapper, mock(ProductReviewRunMapper.class),
                new ProductReviewProperties(), mock(ProductReviewAgentService.class), mock(ProductReviewPersistenceService.class));

        service.requestManualReview(1L, 2L);

        verify(productMapper).insertSellerAppealLog(1L, "卖家申请人工复核");
        when(productMapper.selectLatestReviewOperatorType(1L)).thenReturn(0);
        assertThrows(BizException.class, () -> service.requestManualReview(1L, 2L));
    }

    @Test
    void concurrentAppealUsesConditionalUpdate() {
        ProductMapper productMapper = mock(ProductMapper.class);
        Product product = new Product();
        product.setId(1L);
        product.setSellerId(2L);
        product.setStatus(2);
        when(productMapper.selectById(1L)).thenReturn(Optional.of(product));
        when(productMapper.selectLatestReviewOperatorType(1L)).thenReturn(1);
        when(productMapper.appealAiRejectBySeller(1L, 2L)).thenReturn(0);
        ProductReviewRunService service = service(productMapper, mock(ProductReviewRunMapper.class),
                new ProductReviewProperties(), mock(ProductReviewAgentService.class), mock(ProductReviewPersistenceService.class));

        assertThrows(BizException.class, () -> service.requestManualReview(1L, 2L));
        verify(productMapper, never()).insertSellerAppealLog(anyLong(), anyString());
    }

    private ProductReviewRunService service(ProductMapper productMapper, ProductReviewRunMapper runMapper,
                                            ProductReviewProperties properties, ProductReviewAgentService agent,
                                            ProductReviewPersistenceService persistence) {
        return new ProductReviewRunService(productMapper, runMapper, properties, mock(ProductReviewRuleService.class),
                agent, mock(ProductDetailCacheService.class), new ObjectMapper(), mock(Executor.class), persistence);
    }

    private ProductReviewRunEntity runningRun() {
        ProductReviewRunEntity run = new ProductReviewRunEntity();
        run.setRunId("run");
        run.setProductId(1L);
        run.setSubmittedProductVersion(2);
        run.setInputSnapshot("{}");
        return run;
    }

    private ProductReviewResult rejectResult(double confidence) {
        return new ProductReviewResult(ProductReviewDecision.REJECT, ProductReviewRiskLevel.HIGH, confidence,
                List.of("违规"), List.of("修改"), List.of(
                new ProductReviewResult.RuleRef("PRODUCT-001", "2026-01", "规则", "违规")));
    }

}
