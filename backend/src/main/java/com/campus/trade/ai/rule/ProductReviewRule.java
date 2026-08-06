package com.campus.trade.ai.rule;

import java.time.OffsetDateTime;

/**
 * A validated rule fragment loaded from the product review rule source.
 */
public record ProductReviewRule(
        String ruleId,
        String version,
        String effectiveAt,
        OffsetDateTime effectiveAtTime,
        String domain,
        String title,
        String body,
        String bodySha256
) {
}
