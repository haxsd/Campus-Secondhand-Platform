package com.campus.trade.ai.review;

import java.time.LocalDateTime;
import java.util.List;

public record AdminProductReviewVO(
        Long productId,
        Integer productStatus,
        ProductReviewRunDetail latestRun
) {
    public record ProductReviewRunDetail(
            String runId,
            String status,
            ProductReviewDecision decision,
            ProductReviewRiskLevel riskLevel,
            Double confidence,
            List<String> reasons,
            List<String> suggestions,
            List<ProductReviewResult.RuleRef> ruleRefs,
            LocalDateTime finishedAt
    ) {
    }
}
