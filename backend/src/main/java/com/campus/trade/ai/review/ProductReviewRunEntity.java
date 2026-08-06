package com.campus.trade.ai.review;

import java.time.LocalDateTime;

public class ProductReviewRunEntity {
    private Long id;
    private String runId;
    private String agentType;
    private Long productId;
    private Long sellerId;
    private Integer submittedProductVersion;
    private String ruleVersion;
    private String modelName;
    private String status;
    private Integer attempt;
    private String decision;
    private String riskLevel;
    private Double confidence;
    private String inputSnapshot;
    private String resultJson;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Integer getSubmittedProductVersion() { return submittedProductVersion; }
    public void setSubmittedProductVersion(Integer value) { this.submittedProductVersion = value; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String value) { this.ruleVersion = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { this.modelName = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Integer getAttempt() { return attempt; }
    public void setAttempt(Integer value) { this.attempt = value; }
    public String getDecision() { return decision; }
    public void setDecision(String value) { this.decision = value; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String value) { this.riskLevel = value; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double value) { this.confidence = value; }
    public String getInputSnapshot() { return inputSnapshot; }
    public void setInputSnapshot(String value) { this.inputSnapshot = value; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String value) { this.resultJson = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { this.errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { this.errorMessage = value; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime value) { this.startedAt = value; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime value) { this.finishedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
