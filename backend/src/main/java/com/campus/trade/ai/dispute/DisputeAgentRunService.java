package com.campus.trade.ai.dispute;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 纠纷 Agent 的触发、幂等和异步编排。
 *
 * <p>run 在请求事务中创建，只有事务提交后才投递线程池；否则事务回滚时，
 * 异步线程可能看到一个尚未提交的输入，造成“模型已调用但数据库没有 run”的审计断裂。</p>
 */
@Service
public class DisputeAgentRunService {
    private final DisputeService disputeService;
    private final DisputeAgentRunMapper runMapper;
    private final DisputeAgentSnapshotBuilder snapshotBuilder;
    private final DisputeAgentModelService modelService;
    private final DisputeAgentPersistenceService persistenceService;
    private final DisputeAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final DisputeRuleService ruleService;
    private final Executor executor;

    public DisputeAgentRunService(
            DisputeService disputeService, DisputeAgentRunMapper runMapper,
            DisputeAgentSnapshotBuilder snapshotBuilder, DisputeAgentModelService modelService,
            DisputeAgentPersistenceService persistenceService, DisputeAgentProperties properties,
            ObjectMapper objectMapper, DisputeRuleService ruleService,
            @Qualifier("disputeAiAssistExecutor") Executor executor
    ) {
        this.disputeService = disputeService;
        this.runMapper = runMapper;
        this.snapshotBuilder = snapshotBuilder;
        this.modelService = modelService;
        this.persistenceService = persistenceService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.ruleService = ruleService;
        this.executor = executor;
    }

    /**
     * 管理员手动触发一次分析。
     *
     * <p>幂等键是 disputeId + evidenceVersion；证据版本不变时重复点击不能重复消耗模型 token。</p>
     */
    @Transactional
    public DisputeAgentRunView trigger(Long disputeId) {
        if (!properties.isEnabled()) {
            return new DisputeAgentRunView(null, "DISABLED", null, null, null, "纠纷 AI 辅助已关闭", null);
        }
        DisputeDetailVO detail = disputeService.adminDetail(disputeId);
        if (!List.of(0, 1).contains(detail.status())) {
            throw new BizException(ErrorCode.CONFLICT, "已裁决纠纷不能触发 AI 辅助");
        }
        DisputeAgentRunEntity existing = runMapper.selectSucceeded(disputeId, detail.evidenceVersion());
        if (existing != null) {
            return DisputeAgentRunView.from(existing);
        }
        DisputeAgentInputSnapshot snapshot = snapshotBuilder.build(detail);
        String snapshotJson = writeSnapshot(snapshot);
        DisputeAgentRunEntity run = new DisputeAgentRunEntity();
        run.setRunId(UUID.randomUUID().toString());
        run.setDisputeId(disputeId);
        run.setOrderId(detail.orderId());
        run.setStatus(DisputeAgentStatus.PENDING.name());
        run.setAttempt(0);
        run.setModelName(properties.getModel());
        run.setRuleVersion(ruleService.currentVersion());
        run.setSubmittedEvidenceVersion(detail.evidenceVersion());
        run.setInputSnapshot(snapshotJson);
        run.setInputDigest(sha256(snapshotJson));
        run.setTriggeredBy(UserContext.requireCurrentUser().userId());
        runMapper.insert(run);
        registerAfterCommit(run);
        return DisputeAgentRunView.from(run);
    }

    /** 查询最新 run，供管理员轮询。 */
    public DisputeAgentRunView latest(Long disputeId) {
        return DisputeAgentRunView.from(runMapper.selectLatest(disputeId));
    }

    /**
     * 记录管理员采纳建议。
     *
     * <p>采纳只写审计字段，真实裁决必须继续通过 DisputeService.handle。</p>
     */
    @Transactional
    public DisputeAgentRunView adopt(Long disputeId, String runId, String action) {
        DisputeAgentRunEntity run = runMapper.selectByRunId(runId);
        if (run == null || !Objects.equals(run.getDisputeId(), disputeId)
                || !DisputeAgentStatus.SUCCEEDED.name().equals(run.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "当前没有可采纳的有效建议");
        }
        runMapper.markAdopted(runId, UserContext.requireCurrentUser().userId(), action);
        return DisputeAgentRunView.from(runMapper.selectByRunId(runId));
    }

    /**
     * 异步线程执行模型调用。公开方法便于测试验证 afterCommit 之后才投递任务。
     */
    public void execute(DisputeAgentRunEntity run) {
        if (runMapper.markRunning(run.getRunId()) == 0) return;
        try {
            DisputeDetailVO current = disputeService.adminDetail(run.getDisputeId());
            if (!isCurrent(current, run)) {
                persistenceService.stale(run);
                return;
            }
            DisputeAgentInputSnapshot snapshot = readSnapshot(run.getInputSnapshot());
            DisputeAgentResult result = analyzeWithRetry(snapshot);
            current = disputeService.adminDetail(run.getDisputeId());
            if (!isCurrent(current, run)) {
                persistenceService.stale(run);
                return;
            }
            persistenceService.complete(run, result);
        } catch (DisputeAgentTimeoutException exception) {
            persistenceService.timeout(run);
        } catch (DisputeAgentOutputInvalidException exception) {
            persistenceService.fail(
                    run,
                    DisputeAgentStatus.INVALID_OUTPUT,
                    "AI_OUTPUT_INVALID",
                    exception.getMessage(),
                    exception.getRawResponse()
            );
        } catch (DisputeAgentRetryableException exception) {
            persistenceService.fail(run, DisputeAgentStatus.FAILED, "RETRY_EXHAUSTED", exception.getMessage());
        } catch (RuntimeException exception) {
            persistenceService.fail(run, DisputeAgentStatus.FAILED, "AI_FAILED", exception.getMessage());
        }
    }

    private DisputeAgentResult analyzeWithRetry(DisputeAgentInputSnapshot snapshot) {
        int attempts = 0;
        while (true) {
            try {
                return modelService.analyze(snapshot, snapshotBuilder.factFields(snapshot));
            } catch (DisputeAgentRetryableException exception) {
                if (attempts >= properties.getMaxRetries()) throw exception;
                attempts++;
                backoff();
            }
        }
    }

    private void backoff() {
        try {
            Thread.sleep(properties.getRetryBackoff().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DisputeAgentRetryableException("重试等待被中断", exception);
        }
    }

    private void registerAfterCommit(DisputeAgentRunEntity run) {
        Runnable task = () -> execute(run);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
        } else {
            executor.execute(task);
        }
    }

    private boolean isCurrent(DisputeDetailVO detail, DisputeAgentRunEntity run) {
        return List.of(0, 1).contains(detail.status())
                && Objects.equals(detail.evidenceVersion(), run.getSubmittedEvidenceVersion());
    }

    private DisputeAgentInputSnapshot readSnapshot(String value) {
        try {
            return objectMapper.readValue(value, DisputeAgentInputSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 输入快照读取失败", exception);
        }
    }

    private String writeSnapshot(DisputeAgentInputSnapshot value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 输入快照序列化失败", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
