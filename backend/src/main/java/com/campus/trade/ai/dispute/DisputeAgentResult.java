package com.campus.trade.ai.dispute;

import java.util.List;

/**
 * 纠纷 Agent 的严格结构化输出。
 *
 * <p>这些字段只用于辅助管理员判断，服务不会根据它们修改订单或纠纷状态。</p>
 */
public record DisputeAgentResult(
        DisputeAgentAction suggestedAction,
        Double confidence,
        DisputeAgentLiability liability,
        List<String> reasons,
        List<VerifiedFact> verifiedFacts,
        List<String> missingEvidence,
        Boolean suggestedRestock,
        List<RuleRef> ruleRefs,
        String adminSummary
) {
    public record VerifiedFact(String field, String quote) { }
    public record RuleRef(String ruleId, String ruleVersion, String title, String evidence) { }
}
