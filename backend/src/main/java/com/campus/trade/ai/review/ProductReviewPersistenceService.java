package com.campus.trade.ai.review;

import com.campus.trade.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReviewPersistenceService {
    private final ProductReviewRunMapper runMapper;
    private final ProductMapper productMapper;

    public ProductReviewPersistenceService(ProductReviewRunMapper runMapper, ProductMapper productMapper) {
        this.runMapper = runMapper;
        this.productMapper = productMapper;
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
}
