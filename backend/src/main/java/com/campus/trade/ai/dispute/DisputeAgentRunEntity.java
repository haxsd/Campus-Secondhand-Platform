package com.campus.trade.ai.dispute;

import java.time.LocalDateTime;

/**
 * dispute_agent_run 的持久化对象。
 *
 * <p>run 只记录分析输入、模型结果和管理员采纳信息，不持有修改交易数据的权限。</p>
 */
public class DisputeAgentRunEntity {
    private Long id;
    private String runId;
    private Long disputeId;
    private Long orderId;
    private String status;
    private Integer attempt;
    private String modelName;
    private String ruleVersion;
    private Integer submittedEvidenceVersion;
    private String inputSnapshot;
    private String inputDigest;
    private String resultJson;
    private String errorCode;
    private String errorMessage;
    private Long triggeredBy;
    private Long adoptedBy;
    private LocalDateTime adoptedAt;
    private String adoptedAction;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public Long getDisputeId() { return disputeId; }
    public void setDisputeId(Long disputeId) { this.disputeId = disputeId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAttempt() { return attempt; }
    public void setAttempt(Integer attempt) { this.attempt = attempt; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public Integer getSubmittedEvidenceVersion() { return submittedEvidenceVersion; }
    public void setSubmittedEvidenceVersion(Integer value) { this.submittedEvidenceVersion = value; }
    public String getInputSnapshot() { return inputSnapshot; }
    public void setInputSnapshot(String inputSnapshot) { this.inputSnapshot = inputSnapshot; }
    public String getInputDigest() { return inputDigest; }
    public void setInputDigest(String inputDigest) { this.inputDigest = inputDigest; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(Long triggeredBy) { this.triggeredBy = triggeredBy; }
    public Long getAdoptedBy() { return adoptedBy; }
    public void setAdoptedBy(Long adoptedBy) { this.adoptedBy = adoptedBy; }
    public LocalDateTime getAdoptedAt() { return adoptedAt; }
    public void setAdoptedAt(LocalDateTime adoptedAt) { this.adoptedAt = adoptedAt; }
    public String getAdoptedAction() { return adoptedAction; }
    public void setAdoptedAction(String adoptedAction) { this.adoptedAction = adoptedAction; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
