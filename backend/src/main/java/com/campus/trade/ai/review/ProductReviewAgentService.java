package com.campus.trade.ai.review;

import com.campus.trade.ai.rule.ProductReviewRule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductReviewAgentService {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    private final ProductReviewPromptService promptService;
    private final ProductReviewRuleService ruleService;
    private final ProductReviewValidator validator;
    private final ObjectMapper objectMapper;
    private final ProductReviewProperties properties;

    public ProductReviewAgentService(
            ObjectProvider<ChatClient.Builder> chatClientBuilder,
            ProductReviewPromptService promptService,
            ProductReviewRuleService ruleService,
            ProductReviewValidator validator,
            ObjectMapper objectMapper,
            ProductReviewProperties properties
    ) {
        this.chatClientBuilder = chatClientBuilder;
        this.promptService = promptService;
        this.ruleService = ruleService;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public ProductReviewResult review(Map<String, Object> snapshot) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("AI ChatClient未配置");
        }
        List<ProductReviewRule> rules = ruleService.currentRules();
        Set<String> allowed = new HashSet<>();
        rules.forEach(rule -> allowed.add(rule.ruleId() + "@" + rule.version()));
        String raw = builder.build().prompt()
                .options(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null))
                        .build())
                .system(promptService.systemPrompt())
                .user(promptService.userMessage(snapshot, ruleService.currentVersion(), rules))
                .call()
                .content();
        if (raw == null || raw.isBlank()) {
            throw new ProductReviewOutputInvalidException("AI_OUTPUT_INVALID:模型返回为空");
        }
        try {
            ObjectMapper strict = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            ProductReviewResult result = strict.readValue(raw, ProductReviewResult.class);
            validator.validate(result, allowed, snapshot);
            return result;
        } catch (Exception exception) {
            throw new ProductReviewOutputInvalidException("AI_OUTPUT_INVALID:结构化输出校验失败", exception);
        }
    }
}
