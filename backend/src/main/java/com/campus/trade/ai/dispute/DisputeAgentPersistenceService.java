package com.campus.trade.ai.dispute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 纠纷 Agent 运行结果的独立持久化服务。
 *
 * <p>失败和超时必须在独立事务中落库，不能因为请求事务已经结束或模型线程异常而丢失审计记录。</p>
 */
@Service
public class DisputeAgentPersistenceService {
    private static final int RAW_RESPONSE_LIMIT = 4000;

    private final DisputeAgentRunMapper runMapper;
    private final ObjectMapper objectMapper;

    public DisputeAgentPersistenceService(DisputeAgentRunMapper runMapper, ObjectMapper objectMapper) {
        this.runMapper = runMapper;
        this.objectMapper = objectMapper;
    }

    /** 写入成功结果；调用方会在写入前再次检查证据版本。 */
    @Transactional
    public boolean complete(DisputeAgentRunEntity run, DisputeAgentResult result) {
        try {
            return runMapper.markSuccess(run.getRunId(), objectMapper.writeValueAsString(result)) > 0;
        } catch (JsonProcessingException exception) {
            fail(run, DisputeAgentStatus.FAILED, "RESULT_SERIALIZE_FAILED", exception.getMessage(), null);
            return false;
        }
    }

    /** 持久化输出非法、网络失败等终态。 */
    @Transactional
    public void fail(DisputeAgentRunEntity run, DisputeAgentStatus status, String code, String message) {
        fail(run, status, code, message, null);
    }

    /** 持久化终态及截断后的模型原文，避免错误信息丢失诊断上下文。 */
    @Transactional
    public void fail(
            DisputeAgentRunEntity run,
            DisputeAgentStatus status,
            String code,
            String message,
            String rawResponse
    ) {
        runMapper.markFailure(
                run.getRunId(),
                status.name(),
                code,
                limit(message, 500),
                limit(rawResponse, RAW_RESPONSE_LIMIT)
        );
    }

    /** 超时是独立终态，不能伪装成普通失败或输出非法。 */
    @Transactional
    public void timeout(DisputeAgentRunEntity run) {
        fail(run, DisputeAgentStatus.TIMEOUT, "AI_TIMEOUT", "模型调用超过允许时长", null);
    }

    /** 证据版本或纠纷状态变化后，使运行结果失效。 */
    @Transactional
    public void stale(DisputeAgentRunEntity run) {
        runMapper.markStale(run.getRunId(), "VERSION_OR_STATUS_CHANGED");
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
