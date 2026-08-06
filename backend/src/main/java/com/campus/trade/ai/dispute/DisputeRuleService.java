package com.campus.trade.ai.dispute;

import com.campus.trade.ai.rule.ProductReviewRule;
import com.campus.trade.ai.rule.ProductReviewRuleParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 加载并缓存纠纷规则。
 *
 * <p>解析器本身是稳定的 front matter 基础能力，但这里转换成独立的 DisputeRule，
 * 业务代码不会直接依赖商品审核的规则实体。</p>
 */
@Service
public class DisputeRuleService {
    private static final String RULE_RESOURCE = "ai-rules/dispute-rules-2026-01.md";
    private static final String RULE_DOMAIN = "DISPUTE_RULE";

    private volatile List<DisputeRule> cachedRules;

    /** 返回当前发布的纠纷规则。首次读取后缓存，避免每次模型调用都访问 classpath。 */
    public List<DisputeRule> currentRules() {
        List<DisputeRule> result = cachedRules;
        if (result != null) {
            return result;
        }
        try {
            ClassPathResource resource = new ClassPathResource(RULE_RESOURCE);
            String source = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<ProductReviewRule> parsedRules = new ProductReviewRuleParser(RULE_DOMAIN).parse(source);
            result = parsedRules.stream().map(this::toDisputeRule).toList();
            cachedRules = List.copyOf(result);
            return cachedRules;
        } catch (Exception exception) {
            throw new IllegalStateException("纠纷规则加载失败", exception);
        }
    }

    /** 返回规则集合版本，用于 run 审计和结果解释。 */
    public String currentVersion() {
        return currentRules().get(0).version();
    }

    private DisputeRule toDisputeRule(ProductReviewRule rule) {
        return new DisputeRule(
                rule.ruleId(), rule.version(), rule.effectiveAt(),
                rule.title(), rule.body(), rule.bodySha256()
        );
    }
}
