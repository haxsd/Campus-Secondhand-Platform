package com.campus.trade.ai.review;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public final class ProductReviewValidator {
    public void validate(ProductReviewResult result, Set<String> allowedRuleRefs) {
        validate(result, allowedRuleRefs, Map.of());
    }

    public void validate(ProductReviewResult result, Set<String> allowedRuleRefs,
                         Map<String, Object> snapshot) {
        if (result == null || result.decision() == null || result.riskLevel() == null) {
            throw invalid("decision/riskLevel不能为空");
        }
        if (result.confidence() == null || result.confidence() < 0 || result.confidence() > 1) {
            throw invalid("confidence必须在[0,1]范围内");
        }
        validateList(result.reasons(), 10, true, "reasons");
        validateList(result.suggestions(), 10, false, "suggestions");
        if (result.ruleRefs() == null) {
            throw invalid("ruleRefs不能为空");
        }
        for (ProductReviewResult.RuleRef ref : result.ruleRefs()) {
            if (ref == null || ref.ruleId() == null || ref.ruleVersion() == null
                    || !allowedRuleRefs.contains(ref.ruleId() + "@" + ref.ruleVersion())
                    || ref.evidence() != null && ref.evidence().length() > 200) {
                throw invalid("ruleRefs包含未注入规则或证据过长");
            }
        }
        if (result.decision() == ProductReviewDecision.REJECT
                && (result.reasons().isEmpty() || result.ruleRefs().isEmpty())) {
            throw invalid("REJECT必须提供原因和规则引用");
        }
        if (result.decision() == ProductReviewDecision.REJECT && !snapshot.isEmpty()) {
            String source = String.valueOf(snapshot.getOrDefault("title", ""))
                    + "\n" + String.valueOf(snapshot.getOrDefault("description", ""));
            if (result.ruleRefs().stream().anyMatch(ref -> ref.evidence() == null
                    || ref.evidence().isBlank()
                    || !source.contains(ref.evidence()))) {
                throw invalid("REJECT规则引用必须提供商品原文证据");
            }
        }
    }

    private void validateList(List<String> values, int max, boolean required, String name) {
        if (values == null || values.size() > max || (required && values.isEmpty())
                || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw invalid(name + "不能为空且数量不能超过限制");
        }
    }

    private ProductReviewOutputInvalidException invalid(String message) {
        return new ProductReviewOutputInvalidException(message);
    }
}
