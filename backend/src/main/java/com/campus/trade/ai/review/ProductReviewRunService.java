package com.campus.trade.ai.review;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.model.ProductStatus;
import com.campus.trade.product.service.ProductDetailCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ProductReviewRunService {
    private final ProductMapper productMapper;
    private final ProductReviewRunMapper runMapper;
    private final ProductReviewProperties properties;
    private final ProductReviewRuleService ruleService;
    private final ProductReviewAgentService agentService;
    private final ProductDetailCacheService cacheService;
    private final ObjectMapper objectMapper;
    private final Executor productAiReviewExecutor;
    private final ProductReviewPersistenceService persistenceService;

    public ProductReviewRunService(ProductMapper productMapper, ProductReviewRunMapper runMapper,
                                   ProductReviewProperties properties, ProductReviewRuleService ruleService,
                                   ProductReviewAgentService agentService, ProductDetailCacheService cacheService,
                                   ObjectMapper objectMapper,
                                   @Qualifier("productAiReviewExecutor") Executor productAiReviewExecutor,
                                   ProductReviewPersistenceService persistenceService) {
        this.productMapper = productMapper;
        this.runMapper = runMapper;
        this.properties = properties;
        this.ruleService = ruleService;
        this.agentService = agentService;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.productAiReviewExecutor = productAiReviewExecutor;
        this.persistenceService = persistenceService;
    }

    @Transactional
    public ProductReviewSubmitVO submit(Long productId, Long sellerId) {
        Product product = productMapper.selectById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!sellerId.equals(product.getSellerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该商品");
        }
        if (!properties.isEnabled() && product.getStatus() == ProductStatus.AI_REVIEWING.getCode()) {
            if (productMapper.disableAiReviewBySeller(productId, sellerId) == 0) {
                throw new BizException(ErrorCode.CONFLICT, "商品状态已变化，请刷新后重试");
            }
            int version = productMapper.selectVersion(productId);
            registerCacheInvalidation(productId);
            return new ProductReviewSubmitVO(productId, ProductStatus.PENDING_REVIEW.getCode(), null, version, false);
        }
        if (product.getStock() == null || product.getStock() <= 0
                || !List.of(ProductStatus.DRAFT.getCode(), ProductStatus.REJECTED.getCode(),
                ProductStatus.OFF_SHELF.getCode(), ProductStatus.AI_REVIEWING.getCode()).contains(product.getStatus())) {
            if (product.getStatus() == ProductStatus.AI_REVIEWING.getCode()) {
                ProductReviewRunEntity existing = runMapper.selectLatestByProductId(productId);
                return toSubmit(product, existing);
            }
            throw new BizException(ErrorCode.CONFLICT, "当前商品状态不能提交审核");
        }
        if (!properties.isEnabled()) {
            if (productMapper.submitReviewBySeller(productId, sellerId) == 0) {
                throw new BizException(ErrorCode.CONFLICT, "商品状态已变化，请刷新后重试");
            }
            int version = productMapper.selectVersion(productId);
            return new ProductReviewSubmitVO(productId, ProductStatus.PENDING_REVIEW.getCode(), null, version, false);
        }
        if (product.getStatus() == ProductStatus.AI_REVIEWING.getCode()) {
            return toSubmit(product, runMapper.selectLatestByProductId(productId));
        }
        if (productMapper.submitAiReviewBySeller(productId, sellerId) == 0) {
            throw new BizException(ErrorCode.CONFLICT, "商品状态已变化，请刷新后重试");
        }
        int submittedVersion = productMapper.selectVersion(productId);
        String runId = "pr_" + UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> snapshot = snapshot(product, submittedVersion);
        ProductReviewRunEntity run = new ProductReviewRunEntity();
        run.setRunId(runId);
        run.setAgentType("PRODUCT_REVIEW");
        run.setProductId(productId);
        run.setSellerId(sellerId);
        run.setSubmittedProductVersion(submittedVersion);
        run.setRuleVersion(ruleService.currentVersion());
        run.setModelName(properties.getModel());
        run.setStatus(ProductReviewRunStatus.PENDING.name());
        run.setAttempt(0);
        try {
            run.setInputSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI输入快照序列化失败", exception);
        }
        runMapper.insert(run);
        registerAfterCommit(runId);
        registerCacheInvalidation(productId);
        return new ProductReviewSubmitVO(productId, ProductStatus.AI_REVIEWING.getCode(), runId, submittedVersion, true);
    }

    @Async("productAiReviewExecutor")
    public void executeAfterCommit(String runId) {
        ProductReviewRunEntity run = runMapper.selectByRunId(runId);
        if (run == null || runMapper.markRunning(runId) == 0) return;
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    run.getInputSnapshot(), new TypeReference<Map<String, Object>>() {});
            ProductReviewResult result = reviewWithRetry(snapshot);
            String resultJson = objectMapper.writeValueAsString(result);
            runMapper.markSuccess(runId, result.decision().name(), result.riskLevel().name(),
                    result.confidence(), resultJson);
            productMapper.insertAiReviewLog(run.getProductId(), runId,
                    result.decision() == ProductReviewDecision.PASS ? 1
                            : result.decision() == ProductReviewDecision.REJECT ? 2 : 3,
                    String.join("；", result.reasons()));
            if (productMapper.completeAiReview(run.getProductId(), run.getSubmittedProductVersion()) == 0) {
                runMapper.markStale(runId, "VERSION_OR_STATUS_CHANGED");
            } else {
                registerCacheInvalidation(run.getProductId());
            }
        } catch (ProductReviewOutputInvalidException exception) {
            fail(run, ProductReviewRunStatus.INVALID_OUTPUT, "AI_OUTPUT_INVALID", "模型输出校验失败");
        } catch (Exception exception) {
            fail(run, ProductReviewRunStatus.FAILED, "AI_EXECUTION_FAILED", "AI审核执行失败");
        }
    }

    private ProductReviewResult reviewWithRetry(Map<String, Object> snapshot) {
        int attempts = 0;
        while (true) {
            try {
                return agentService.review(snapshot);
            } catch (IllegalArgumentException exception) {
                if (attempts++ >= Math.min(1, properties.getMaxRetries())) throw exception;
                sleepBeforeRetry();
            } catch (Exception exception) {
                if (!isRetryable(exception) || attempts++ >= properties.getMaxRetries()) throw exception;
                sleepBeforeRetry();
            }
        }
    }

    private boolean isRetryable(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof RestClientResponseException response) {
                int status = response.getStatusCode().value();
                return status == 429 || status >= 500;
            }
            if (current instanceof SocketTimeoutException || current instanceof TimeoutException) return true;
            current = current.getCause();
        }
        return false;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(properties.getRetryBackoff().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI retry interrupted", exception);
        }
    }

    private void fail(ProductReviewRunEntity run, ProductReviewRunStatus status, String code, String message) {
        persistenceService.fail(run, status, code, message);
        registerCacheInvalidation(run.getProductId());
    }

    @Transactional
    public void fallbackStaleRuns() {
        LocalDateTime cutoff = LocalDateTime.now().minus(properties.getStaleAfter());
        runMapper.selectStale(cutoff, 100).forEach(run -> {
            persistenceService.timeout(run);
            registerCacheInvalidation(run.getProductId());
        });
    }

    public ProductReviewRunVO getRun(String runId) {
        ProductReviewRunEntity run = runMapper.selectByRunId(runId);
        if (run == null) throw new BizException(ErrorCode.NOT_FOUND, "审核运行不存在");
        Product product = productMapper.selectById(run.getProductId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!UserContext.requireCurrentUser().userId().equals(product.getSellerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该审核");
        }
        return new ProductReviewRunVO(run.getRunId(), run.getProductId(), run.getStatus(),
                enumValue(ProductReviewDecision.class, run.getDecision()), enumValue(ProductReviewRiskLevel.class, run.getRiskLevel()),
                run.getConfidence(), product.getStatus(), run.getFinishedAt(), run.getErrorCode());
    }

    public ProductReviewRunVO getLatestRun(Long productId) {
        ProductReviewRunEntity run = runMapper.selectLatestByProductId(productId);
        if (run == null) throw new BizException(ErrorCode.NOT_FOUND, "审核运行不存在");
        return getRun(run.getRunId());
    }

    public AdminProductReviewVO getAdminReview(Long productId) {
        Product product = productMapper.selectById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        ProductReviewRunEntity run = runMapper.selectLatestByProductId(productId);
        if (run == null || run.getResultJson() == null) return new AdminProductReviewVO(productId, product.getStatus(), null);
        try {
            ProductReviewResult result = objectMapper.readValue(run.getResultJson(), ProductReviewResult.class);
            return new AdminProductReviewVO(productId, product.getStatus(),
                    new AdminProductReviewVO.ProductReviewRunDetail(run.getRunId(), run.getStatus(),
                            result.decision(), result.riskLevel(), result.confidence(), result.reasons(),
                            result.suggestions(), result.ruleRefs(), run.getFinishedAt()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI审核结果读取失败", exception);
        }
    }

    private Map<String, Object> snapshot(Product product, int version) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("productId", product.getId());
        snapshot.put("submittedProductVersion", version);
        snapshot.put("sellerId", product.getSellerId());
        snapshot.put("categoryId", product.getCategoryId());
        snapshot.put("title", product.getTitle());
        snapshot.put("description", product.getDescription());
        snapshot.put("price", product.getPrice());
        snapshot.put("stock", product.getStock());
        snapshot.put("itemCondition", product.getItemCondition());
        snapshot.put("campus", product.getCampus());
        snapshot.put("tradePlace", product.getTradePlace());
        snapshot.put("images", productMapper.selectImageUrlsByProductId(product.getId()));
        return snapshot;
    }

    private ProductReviewSubmitVO toSubmit(Product product, ProductReviewRunEntity run) {
        return new ProductReviewSubmitVO(product.getId(), product.getStatus(),
                run == null ? null : run.getRunId(), product.getVersion(), true);
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private void registerCacheInvalidation(Long productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheService.invalidate(productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheService.invalidate(productId);
            }
        });
    }

    private void registerAfterCommit(String runId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productAiReviewExecutor.execute(() -> executeAfterCommit(runId));
            }
        });
    }
}
