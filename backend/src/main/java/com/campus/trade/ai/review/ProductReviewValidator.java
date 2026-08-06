package com.campus.trade.ai.review;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public final class ProductReviewValidator {
    public void validate(ProductReviewResult result, Set<String> allowedRuleRefs) {
        if (result == null || result.decision() == null || result.riskLevel() == null) {
            throw invalid("decision/riskLevel不能为空");
        }
        if (result.confidence() == null || result.confidence() < 0 || result.confidence() > 1) {
            throw invalid("confidence必须在[0,1]");
        }
        validateList(result.reasons(), 10, true, "reasons");
        validateList(result.suggestions(), 10, false, "suggestions");
        if (result.ruleRefs() == null) {
            throw invalid("ruleRefs不能为空");
        }
        for (ProductReviewResult.RuleRef ref : result.ruleRefs()) {
            if (ref == null || ref.ruleId() == null || ref.ruleVersion() == null
                    || !allowedRuleRefs.contains(ref.ruleId() + "@" + ref.ruleVersion())) {
                throw invalid("ruleRefs包含未注入规则");
            }
        }
        if (result.decision() == ProductReviewDecision.REJECT
                && (result.reasons().isEmpty() || result.ruleRefs().isEmpty())) {
            throw invalid("REJECT必须包含原因和有效规则引用");
        }
    }

    private void validateList(List<String> values, int max, boolean required, String name) {
        if (values == null || values.size() > max || (required && values.isEmpty())
                || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw invalid(name + "格式非法");
        }
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
