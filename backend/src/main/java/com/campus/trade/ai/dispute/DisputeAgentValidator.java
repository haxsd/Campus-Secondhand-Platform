package com.campus.trade.ai.dispute;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校验纠纷 Agent 输出。
 *
 * <p>校验不是格式美化，而是防止模型引用未注入规则、编造原文或越权表达，
 * 因为这些错误会直接误导管理员的真实裁决。</p>
 */
@Component
public class DisputeAgentValidator {

    /** 校验结构化建议和它引用的输入快照。 */
    public void validate(
            DisputeAgentResult result,
            Set<String> allowedRuleRefs,
            Map<String, String> snapshotFields
    ) {
        if (result == null || result.suggestedAction() == null || result.liability() == null) {
            throw invalid("suggestedAction 和 liability 不能为空");
        }
        if (result.confidence() == null || result.confidence() < 0 || result.confidence() > 1) {
            throw invalid("confidence 必须在 [0,1] 范围内");
        }
        validateTextList(result.reasons(), 10, true, "reasons");
        validateTextList(result.missingEvidence(), 10, false, "missingEvidence");
        if (result.verifiedFacts() == null || result.verifiedFacts().size() > 10) {
            throw invalid("verifiedFacts 数量不能超过 10 条");
        }
        if (result.ruleRefs() == null || result.ruleRefs().size() > 10) {
            throw invalid("ruleRefs 数量不能超过 10 条");
        }
        validateRuleRefs(result.ruleRefs(), allowedRuleRefs);
        validateVerifiedFacts(result.verifiedFacts(), snapshotFields);
        validateActionEvidence(result);
        validateNoSqlOrPrivilegeText(result);
    }

    private void validateTextList(List<String> values, int max, boolean required, String fieldName) {
        if (values == null || values.size() > max || required && values.isEmpty()
                || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw invalid(fieldName + " 不能为空且最多 10 条");
        }
    }

    private void validateRuleRefs(List<DisputeAgentResult.RuleRef> ruleRefs, Set<String> allowedRuleRefs) {
        for (DisputeAgentResult.RuleRef ruleRef : ruleRefs) {
            if (ruleRef == null || ruleRef.ruleId() == null || ruleRef.ruleVersion() == null
                    || !allowedRuleRefs.contains(ruleRef.ruleId() + "@" + ruleRef.ruleVersion())) {
                throw invalid("ruleRefs 包含本次没有注入的规则");
            }
        }
    }

    private void validateVerifiedFacts(
            List<DisputeAgentResult.VerifiedFact> facts,
            Map<String, String> snapshotFields
    ) {
        for (DisputeAgentResult.VerifiedFact fact : facts) {
            if (fact == null || fact.field() == null || fact.quote() == null || fact.quote().isBlank()) {
                throw invalid("verifiedFacts 的 field 和 quote 不能为空");
            }
            String source = snapshotFields.get(fact.field());
            if (source == null || !source.contains(fact.quote())) {
                throw invalid("verifiedFacts.quote 不是输入快照对应字段的原文");
            }
        }
    }

    private void validateActionEvidence(DisputeAgentResult result) {
        boolean needsEvidence = result.suggestedAction() == DisputeAgentAction.REJECT
                || result.suggestedAction() == DisputeAgentAction.CANCEL_TRADE;
        if (needsEvidence && (result.ruleRefs().isEmpty() || result.verifiedFacts().isEmpty())) {
            throw invalid("REJECT 或 CANCEL_TRADE 必须提供规则和原文事实");
        }
        if (result.suggestedAction() == DisputeAgentAction.NEED_MORE
                && result.missingEvidence().isEmpty()) {
            throw invalid("NEED_MORE 必须提供 missingEvidence");
        }
    }

    private void validateNoSqlOrPrivilegeText(DisputeAgentResult result) {
        String summary = result.adminSummary() == null ? "" : result.adminSummary();
        if (summary.matches("(?is).*\b(select|update|delete|insert|drop)\b.*")
                || summary.contains("管理员账号") || summary.contains("直接修改订单")) {
            throw invalid("输出包含 SQL 或越权指令");
        }
    }

    private DisputeAgentOutputInvalidException invalid(String message) {
        return new DisputeAgentOutputInvalidException(message);
    }
}
