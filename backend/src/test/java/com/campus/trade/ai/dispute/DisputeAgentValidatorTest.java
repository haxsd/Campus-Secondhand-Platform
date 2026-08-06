package com.campus.trade.ai.dispute;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisputeAgentValidatorTest {
    private final DisputeAgentValidator validator = new DisputeAgentValidator();

    @Test
    void fabricatedQuoteIsRejected() {
        DisputeAgentResult result = new DisputeAgentResult(
                DisputeAgentAction.REJECT, 0.8, DisputeAgentLiability.RESPONDENT,
                List.of("事实不符"),
                List.of(new DisputeAgentResult.VerifiedFact("applicantStatement", "模型编造")),
                List.of(), false,
                List.of(new DisputeAgentResult.RuleRef("DISPUTE-001", "2026-01", "货不对板", "快照")),
                "摘要"
        );
        assertThatThrownBy(() -> validator.validate(result, Set.of("DISPUTE-001@2026-01"), Map.of("applicantStatement", "原始陈述")))
                .isInstanceOf(DisputeAgentOutputInvalidException.class);
    }

    @Test
    void rejectNeedsRuleAndFact() {
        DisputeAgentResult result = new DisputeAgentResult(
                DisputeAgentAction.REJECT, 0.8, DisputeAgentLiability.RESPONDENT,
                List.of("理由"), List.of(), List.of(), false, List.of(), "摘要"
        );
        assertThatThrownBy(() -> validator.validate(result, Set.of(), Map.of()))
                .isInstanceOf(DisputeAgentOutputInvalidException.class);
    }

    @Test
    void needMoreNeedsMissingEvidence() {
        DisputeAgentResult result = new DisputeAgentResult(
                DisputeAgentAction.NEED_MORE, 0.8, DisputeAgentLiability.UNCLEAR,
                List.of("材料不足"), List.of(), List.of(), false, List.of(), "摘要"
        );
        assertThatThrownBy(() -> validator.validate(result, Set.of(), Map.of()))
                .isInstanceOf(DisputeAgentOutputInvalidException.class);
    }

    @Test
    void ruleOutsideInjectedWhitelistIsRejected() {
        DisputeAgentResult result = new DisputeAgentResult(
                DisputeAgentAction.NEED_MORE, 0.8, DisputeAgentLiability.UNCLEAR,
                List.of("材料不足"), List.of(), List.of("订单状态证明"), false,
                List.of(new DisputeAgentResult.RuleRef("DISPUTE-999", "2026-01", "越权规则", "")), "摘要"
        );
        assertThatThrownBy(() -> validator.validate(result, Set.of("DISPUTE-001@2026-01"), Map.of()))
                .isInstanceOf(DisputeAgentOutputInvalidException.class);
    }
}
