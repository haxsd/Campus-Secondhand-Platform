package com.campus.trade.ai.dispute;

/**
 * 纠纷领域规则。
 *
 * <p>规则实体独立于商品审核，避免两个领域的版本和解释语义互相污染。</p>
 */
public record DisputeRule(
        String ruleId, String version, String effectiveAt, String title, String body, String bodySha256
) {
}
