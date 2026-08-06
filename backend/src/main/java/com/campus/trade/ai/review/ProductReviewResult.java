package com.campus.trade.ai.review;

import java.util.List;

public record ProductReviewResult(
        ProductReviewDecision decision,
        ProductReviewRiskLevel riskLevel,
        Double confidence,
        List<String> reasons,
        List<String> suggestions,
        List<RuleRef> ruleRefs
) {
    public record RuleRef(String ruleId, String ruleVersion, String title, String evidence) {
    }
}
