package com.campus.trade.ai.review;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductReviewValidatorTest {
    private final ProductReviewValidator validator = new ProductReviewValidator();
    private static final Set<String> RULES = Set.of("PRODUCT-001@2026-01");

    @Test
    void acceptsPass() {
        assertDoesNotThrow(() -> validator.validate(
                new ProductReviewResult(ProductReviewDecision.PASS, ProductReviewRiskLevel.LOW, 0.8,
                        List.of("未发现明确违规"), List.of(), List.of()), RULES));
    }

    @Test
    void acceptsRejectWithRuleReference() {
        assertDoesNotThrow(() -> validator.validate(
                new ProductReviewResult(ProductReviewDecision.REJECT, ProductReviewRiskLevel.HIGH, 1.0,
                        List.of("命中禁售规则"), List.of("驳回商品"),
                        List.of(new ProductReviewResult.RuleRef("PRODUCT-001", "2026-01", "禁售", "文本命中"))), RULES));
    }

    @Test
    void acceptsManualReview() {
        assertDoesNotThrow(() -> validator.validate(
                new ProductReviewResult(ProductReviewDecision.NEED_MANUAL_REVIEW, ProductReviewRiskLevel.MEDIUM, 0.0,
                        List.of("证据不足"), List.of("补充凭证"), List.of()), RULES));
    }

    @Test
    void rejectsInvalidConfidence() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new ProductReviewResult(ProductReviewDecision.PASS, ProductReviewRiskLevel.LOW, 1.1,
                        List.of("原因"), List.of(), List.of()), RULES));
    }

    @Test
    void rejectsUnknownRuleReference() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new ProductReviewResult(ProductReviewDecision.REJECT, ProductReviewRiskLevel.HIGH, .9,
                        List.of("原因"), List.of(),
                        List.of(new ProductReviewResult.RuleRef("PRODUCT-999", "2026-01", "未知", "证据"))), RULES));
    }

    @Test
    void rejectsRejectWithoutReason() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
                new ProductReviewResult(ProductReviewDecision.REJECT, ProductReviewRiskLevel.HIGH, .9,
                        List.of(), List.of(), List.of()), RULES));
    }
}
