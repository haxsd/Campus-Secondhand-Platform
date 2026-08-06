package com.campus.trade.ai.review;

import com.campus.trade.ai.rule.ProductReviewRule;
import com.campus.trade.ai.rule.ProductReviewRuleParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ProductReviewRuleService {
    private final ProductReviewProperties properties;
    private volatile List<ProductReviewRule> rules;

    public ProductReviewRuleService(ProductReviewProperties properties) {
        this.properties = properties;
    }

    public List<ProductReviewRule> currentRules() {
        List<ProductReviewRule> cached = rules;
        if (cached != null) {
            return cached;
        }
        try {
            ClassPathResource resource = new ClassPathResource("ai-rules/product-rules-2026-01.md");
            cached = new ProductReviewRuleParser(properties.getRuleDomain())
                    .parse(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            rules = List.copyOf(cached);
            return rules;
        } catch (IOException exception) {
            throw new IllegalStateException("商品审核规则加载失败", exception);
        }
    }

    public String currentVersion() {
        return currentRules().get(0).version();
    }

    public OffsetDateTime effectiveAt() {
        return currentRules().get(0).effectiveAtTime();
    }
}
