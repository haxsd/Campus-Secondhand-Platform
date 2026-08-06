package com.campus.trade.ai.dispute;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 负责一次纠纷 Agent 模型调用和输出解析。
 *
 * <p>编排服务会在外层控制重试；这里把网络类故障转换为可识别异常，
 * 避免它们被误记成“模型输出非法”。</p>
 */
@Service
public class DisputeAgentModelService {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;
    private final DisputeAgentPromptService promptService;
    private final DisputeRuleService ruleService;
    private final DisputeAgentValidator validator;
    private final ObjectMapper objectMapper;
    private final DisputeAgentProperties properties;
    private final DisputeAgentFailureClassifier failureClassifier;

    public DisputeAgentModelService(
            ObjectProvider<ChatClient.Builder> chatClientBuilder,
            DisputeAgentPromptService promptService,
            DisputeRuleService ruleService,
            DisputeAgentValidator validator,
            ObjectMapper objectMapper,
            DisputeAgentProperties properties,
            DisputeAgentFailureClassifier failureClassifier
    ) {
        this.chatClientBuilder = chatClientBuilder;
        this.promptService = promptService;
        this.ruleService = ruleService;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.failureClassifier = failureClassifier;
    }

    /** 调用模型并返回通过白名单和原文校验的建议。 */
    /**
     * 调用模型并返回通过白名单和原文校验的建议。
     *
     * @param snapshot 脱敏的结构化快照
     * @param factFields 可被 verifiedFacts 引用的原文字段
     */
    public DisputeAgentResult analyze(
            DisputeAgentInputSnapshot snapshot,
            Map<String, String> factFields
    ) {
        CompletableFuture<DisputeAgentResult> task = CompletableFuture.supplyAsync(
                () -> analyzeNow(snapshot, factFields)
        );
        try {
            return task.get(properties.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            task.cancel(true);
            throw new DisputeAgentTimeoutException("模型调用超过请求时限", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DisputeAgentTimeoutException("模型调用等待被中断", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("模型调用失败", cause);
        }
    }

    private DisputeAgentResult analyzeNow(
            DisputeAgentInputSnapshot snapshot,
            Map<String, String> factFields
    ) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("AI ChatClient 未配置");
        }
        List<DisputeRule> rules = ruleService.currentRules();
        Set<String> allowedRuleRefs = new HashSet<>();
        rules.forEach(rule -> allowedRuleRefs.add(rule.ruleId() + "@" + rule.version()));
        String rawResponse;
        try {
            rawResponse = builder.build().prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(properties.getModel())
                            .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null))
                            .build())
                    .system(promptService.systemPrompt())
                    .user(promptService.userMessage(snapshotToMap(snapshot), ruleService.currentVersion(), rules))
                    .call()
                    .content();
        } catch (RuntimeException exception) {
            throw failureClassifier.classify(exception);
        }
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new DisputeAgentOutputInvalidException("模型返回为空（ChatClient content=null 或空字符串，未获得模型响应体）");
        }
        try {
            ObjectMapper strictMapper = objectMapper.copy()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            String normalizedResponse = stripMarkdownCodeBlock(rawResponse);
            DisputeAgentResult result = strictMapper.readValue(
                    normalizedResponse,
                    DisputeAgentResult.class
            );
            validator.validate(result, allowedRuleRefs, factFields);
            return result;
        } catch (DisputeAgentOutputInvalidException exception) {
            throw new DisputeAgentOutputInvalidException(
                    exception.getMessage(), exception, rawResponse
            );
        } catch (Exception exception) {
            throw new DisputeAgentOutputInvalidException(
                    "结构化输出解析或校验失败: " + exception.getMessage(),
                    exception,
                    rawResponse
            );
        }
    }

    private String stripMarkdownCodeBlock(String rawResponse) {
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd >= 0) {
                return trimmed.substring(firstLineEnd + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private Map<String, Object> snapshotToMap(DisputeAgentInputSnapshot snapshot) {
        return objectMapper.convertValue(snapshot, new TypeReference<Map<String, Object>>() { });
    }
}
