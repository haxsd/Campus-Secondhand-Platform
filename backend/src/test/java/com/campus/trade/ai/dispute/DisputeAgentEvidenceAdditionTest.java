package com.campus.trade.ai.dispute;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

class DisputeAgentEvidenceAdditionTest {
    @Test
    void evidenceAdditionUrlQuoteIsAccepted() {
        DisputeAgentResult result = new DisputeAgentResult(
                DisputeAgentAction.NEED_MORE, 0.8, DisputeAgentLiability.UNCLEAR,
                List.of("insufficient evidence"),
                List.of(new DisputeAgentResult.VerifiedFact("evidenceAdditions", "https://example.com/supplement.jpg")),
                List.of("supplemental material"), false, List.of(), "summary"
        );
        new DisputeAgentValidator().validate(result, Set.of(), Map.of("evidenceAdditions", "supplemental statement https://example.com/supplement.jpg"));
    }
}
