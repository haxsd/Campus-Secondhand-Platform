package com.campus.trade.ai.review;

import com.campus.trade.product.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReviewPersistenceService {
    private final ProductReviewRunMapper runMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    public ProductReviewPersistenceService(ProductReviewRunMapper runMapper, ProductMapper productMapper,
                                           ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void fail(ProductReviewRunEntity run, ProductReviewRunStatus status, String code, String message) {
        runMapper.markFailure(run.getRunId(), status.name(), code, message);
        if (productMapper.fallbackAiReview(run.getProductId(), run.getSubmittedProductVersion()) == 0) {
            runMapper.markStale(run.getRunId(), "VERSION_OR_STATUS_CHANGED");
        }
    }

    @Transactional
    public void timeout(ProductReviewRunEntity run) {
        fail(run, ProductReviewRunStatus.TIMEOUT, "AI_STALE", "AI审核超时兜底");
    }

    @Transactional
    public boolean complete(ProductReviewRunEntity run, ProductReviewResult result, int targetStatus) {
        runMapper.markSuccess(run.getRunId(), result.decision().name(), result.riskLevel().name(),
                result.confidence(), writeResult(result));
        if (productMapper.completeAiReview(run.getProductId(), run.getSubmittedProductVersion(), targetStatus) == 0) {
            runMapper.markStale(run.getRunId(), "VERSION_OR_STATUS_CHANGED");
            return false;
        }
        int logResult = targetStatus == 2 ? 2 : result.decision() == ProductReviewDecision.PASS ? 1 : 3;
        productMapper.insertAiReviewLog(run.getProductId(), run.getRunId(), logResult,
                String.join("；", result.reasons()));
        return true;
    }

    private String writeResult(ProductReviewResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI结果序列化失败", exception);
        }
    }
}
