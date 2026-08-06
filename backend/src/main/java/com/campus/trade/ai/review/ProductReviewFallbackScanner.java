package com.campus.trade.ai.review;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.review", name = "enabled", havingValue = "true")
public class ProductReviewFallbackScanner {
    private final ProductReviewRunService runService;
    public ProductReviewFallbackScanner(ProductReviewRunService runService) { this.runService = runService; }
    @Scheduled(fixedDelayString = "${ai.review.scan-delay-ms:60000}")
    public void scan() { runService.fallbackStaleRuns(); }
}
