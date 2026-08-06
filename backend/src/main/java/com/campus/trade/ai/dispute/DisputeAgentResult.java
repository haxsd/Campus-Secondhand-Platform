package com.campus.trade.ai.dispute;
import java.util.List;
public record DisputeAgentResult(DisputeAgentAction suggestedAction,Double confidence,DisputeAgentLiability liability,List<String> reasons,List<VerifiedFact> verifiedFacts,List<String> missingEvidence,Boolean suggestedRestock,List<RuleRef> ruleRefs,String adminSummary){ public record VerifiedFact(String field,String quote){} public record RuleRef(String ruleId,String ruleVersion,String title,String evidence){} }
