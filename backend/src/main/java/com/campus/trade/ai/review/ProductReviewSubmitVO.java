package com.campus.trade.ai.review;

public record ProductReviewSubmitVO(
        Long productId,
        Integer status,
        String runId,
        Integer submittedProductVersion,
        boolean aiReviewEnabled
) {
}
