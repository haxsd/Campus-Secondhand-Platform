package com.campus.trade.ai.dispute;

import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DisputeAgentRunServiceTest {
    @Mock private DisputeService disputeService;
    @Mock private DisputeAgentRunMapper runMapper;
    @Mock private DisputeAgentSnapshotBuilder snapshotBuilder;
    @Mock private DisputeAgentModelService modelService;
    @Mock private DisputeAgentPersistenceService persistenceService;
    @Mock private Executor executor;
    private final DisputeAgentProperties properties = new DisputeAgentProperties();

    @AfterEach
    void clearContext() {
        UserContext.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void disabledSwitchReturnsWithoutCreatingRun() {
        DisputeAgentRunService service = service();
        DisputeAgentRunView view = service.trigger(1L);
        assertThat(view.status()).isEqualTo("DISABLED");
        verifyNoInteractions(disputeService, runMapper, executor);
    }

    @Test
    void finalizedDisputeCannotTrigger() {
        properties.setEnabled(true);
        when(disputeService.adminDetail(1L)).thenReturn(detail(2, 1));
        assertThatThrownBy(() -> service().trigger(1L)).isInstanceOf(BizException.class);
        verifyNoInteractions(runMapper, executor);
    }

    @Test
    void sameEvidenceVersionReusesSucceededRun() {
        properties.setEnabled(true);
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 1));
        DisputeAgentRunEntity existing = new DisputeAgentRunEntity();
        existing.setRunId("existing");
        existing.setStatus(DisputeAgentStatus.SUCCEEDED.name());
        existing.setSubmittedEvidenceVersion(1);
        when(runMapper.selectSucceeded(1L, 1)).thenReturn(existing);
        DisputeAgentRunView view = service().trigger(1L);
        assertThat(view.runId()).isEqualTo("existing");
        verify(runMapper, never()).insert(any());
        verifyNoInteractions(snapshotBuilder, modelService, executor);
    }

    @Test
    void evidenceVersionChangeCreatesNewRunAndRequestDoesNotCallModel() {
        properties.setEnabled(true);
        UserContext.set(new CurrentUser(9L, 2, "token"));
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 1), detail(0, 2));
        when(runMapper.selectSucceeded(anyLong(), anyInt())).thenReturn(null);
        when(snapshotBuilder.build(any())).thenReturn(emptySnapshot(1), emptySnapshot(2));
        when(runMapper.insert(any())).thenReturn(1);
        DisputeAgentRunService service = service();
        service.trigger(1L);
        service.trigger(1L);
        verify(runMapper, times(2)).insert(any(DisputeAgentRunEntity.class));
        verifyNoInteractions(modelService);
    }

    @Test
    void afterCommitIsRequiredBeforeExecutorSubmission() {
        properties.setEnabled(true);
        UserContext.set(new CurrentUser(9L, 2, "token"));
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 1));
        when(runMapper.selectSucceeded(1L, 1)).thenReturn(null);
        when(snapshotBuilder.build(any())).thenReturn(emptySnapshot(1));
        when(runMapper.insert(any())).thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        service().trigger(1L);
        verifyNoInteractions(executor);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(executor).execute(any(Runnable.class));
    }

    @Test
    void versionChangeMarksRunStaleBeforeModelResult() {
        DisputeAgentRunEntity run = new DisputeAgentRunEntity();
        run.setRunId("run-1");
        run.setDisputeId(1L);
        run.setSubmittedEvidenceVersion(1);
        run.setInputSnapshot("{}");
        when(runMapper.markRunning("run-1")).thenReturn(1);
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 2));
        DisputeAgentRunService service = service();
        service.execute(run);
        verify(persistenceService).stale(run);
        verifyNoInteractions(modelService);
    }

    @Test
    void retryableFailureRetriesOnceThenBecomesFailed() {
        DisputeAgentRunEntity run = executableRun();
        properties.setMaxRetries(1);
        properties.setRetryBackoff(java.time.Duration.ZERO);
        when(runMapper.markRunning("run-1")).thenReturn(1);
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 1), detail(0, 1));
        when(snapshotBuilder.factFields(any())).thenReturn(java.util.Map.of());
        when(modelService.analyze(any(), any())).thenThrow(new DisputeAgentRetryableException("429"));
        service().execute(run);
        verify(modelService, times(2)).analyze(any(), any());
        verify(persistenceService).fail(run, DisputeAgentStatus.FAILED, "RETRY_EXHAUSTED", "429");
    }

    @Test
    void invalidOutputUsesDedicatedStatus() {
        DisputeAgentRunEntity run = executableRun();
        when(runMapper.markRunning("run-1")).thenReturn(1);
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 1));
        when(snapshotBuilder.factFields(any())).thenReturn(java.util.Map.of());
        when(modelService.analyze(any(), any())).thenThrow(new DisputeAgentOutputInvalidException("bad json"));
        service().execute(run);
        verify(persistenceService).fail(run, DisputeAgentStatus.INVALID_OUTPUT, "AI_OUTPUT_INVALID", "bad json", null);
    }

    @Test
    void timeoutUsesDedicatedStatus() {
        DisputeAgentRunEntity run = executableRun();
        when(runMapper.markRunning("run-1")).thenReturn(1);
        when(disputeService.adminDetail(1L)).thenReturn(detail(0, 1));
        when(snapshotBuilder.factFields(any())).thenReturn(java.util.Map.of());
        when(modelService.analyze(any(), any())).thenThrow(new DisputeAgentTimeoutException("timeout", new RuntimeException()));
        service().execute(run);
        verify(persistenceService).timeout(run);
    }

    private DisputeAgentRunEntity executableRun() {
        DisputeAgentRunEntity run = new DisputeAgentRunEntity();
        run.setRunId("run-1");
        run.setDisputeId(1L);
        run.setSubmittedEvidenceVersion(1);
        run.setInputSnapshot("{}");
        return run;
    }

    private DisputeAgentRunService service() {
        return new DisputeAgentRunService(
                disputeService, runMapper, snapshotBuilder, modelService, persistenceService,
                properties, new ObjectMapper(), mock(DisputeRuleService.class), executor
        );
    }

    private DisputeDetailVO detail(int status, int evidenceVersion) {
        return new DisputeDetailVO(
                1L, 10L, 2L, 3L, 1, "陈述", List.of(), status, evidenceVersion,
                null, null, null, null, null, null, null, null, null, null, null, List.of(), List.of()
        );
    }

    private DisputeAgentInputSnapshot emptySnapshot(int evidenceVersion) {
        return new DisputeAgentInputSnapshot(
                1, evidenceVersion, "陈述", List.of(), List.of(), null, null, null, null, null, null, null, List.of()
        );
    }
}
