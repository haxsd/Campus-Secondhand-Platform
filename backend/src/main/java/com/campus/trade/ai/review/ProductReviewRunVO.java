package com.campus.trade.ai.review;

import java.time.LocalDateTime;

public record ProductReviewRunVO(
        String runId,
        Long productId,
        String status,
        ProductReviewDecision decision,
        ProductReviewRiskLevel riskLevel,
        Double confidence,
        Integer productStatus,
        LocalDateTime finishedAt,
        String errorCode
) {
}
