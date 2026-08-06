package com.campus.trade.ai.review;

import com.campus.trade.ai.rule.ProductReviewRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductReviewPromptService {
    private final ObjectMapper objectMapper;

    public ProductReviewPromptService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String systemPrompt() {
        return """
                你是校园二手交易平台的商品合规初审 Agent。只能依据输入的商品快照和 PRODUCT_RULE 规则作出结构化判断，不得编造商品事实、规则或证据。
                AI 只有拦截权，没有自动放行权：PASS 和 NEED_MANUAL_REVIEW 都必须回到人工审核队列，不能让商品自动上架；只有高置信度 REJECT 才允许平台自动驳回。
                信息完整、商品主体明确、没有任何违规信号的普通二手商品应给 PASS。不要因为图片只有一张、描述不是长篇、商品是普通教材或电子产品，或者存在规则要求人工关注的常规字段，就机械地给 NEED_MANUAL_REVIEW。
                NEED_MANUAL_REVIEW 只用于确实存在疑点但证据不足、商品主体无法确认、规则明确要求人工核验，或规则之间可能冲突的情况。
                REJECT 只用于规则明确禁止且证据充分的违规商品。每个 ruleRef 必须使用输入规则中的 ruleId 和 ruleVersion；命中规则时必须填写 evidence，evidence 必须逐字截取自商品标题或描述，不能改写、拼接或凭空生成，长度不超过 200 个字符。
                必须只输出 JSON，不要 Markdown、解释文字或代码围栏。JSON 字段必须为 decision、riskLevel、confidence、reasons、suggestions、ruleRefs。
                decision 只能是 PASS、REJECT、NEED_MANUAL_REVIEW；riskLevel 只能是 LOW、MEDIUM、HIGH；confidence 是 0 到 1 的数字。
                reasons 和 suggestions 是字符串数组；ruleRefs 是对象数组，对象字段为 ruleId、ruleVersion、title、evidence。title 可以复制输入规则标题，evidence 必须是原文片段。
                """;
    }

    public String userMessage(Map<String, Object> snapshot, String ruleVersion, List<ProductReviewRule> rules) {
        Map<String, Object> payload = Map.of(
                "task", "PRODUCT_COMPLIANCE_REVIEW",
                "ruleSet", Map.of("domain", "PRODUCT_RULE", "ruleVersion", ruleVersion),
                "productSnapshot", snapshot,
                "retrievedRules", rules.stream().map(rule -> Map.of(
                        "ruleId", rule.ruleId(), "version", rule.version(), "domain", rule.domain(),
                        "title", rule.title(), "body", rule.body()
                )).toList()
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI输入序列化失败", exception);
        }
    }
}
