package com.campus.trade.dispute.entity;

import java.time.LocalDateTime;

/**
 * 纠纷证据追加流水。每条记录描述一次参与方追加的说明和图片，历史记录永不覆盖。
 */
public class DisputeEvidenceLog {
    private Long id;
    private Long disputeId;
    private Long operatorId;
    private Integer operatorRole;
    private Integer evidenceVersion;
    private String statement;
    private String evidenceJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDisputeId() { return disputeId; }
    public void setDisputeId(Long disputeId) { this.disputeId = disputeId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Integer getOperatorRole() { return operatorRole; }
    public void setOperatorRole(Integer operatorRole) { this.operatorRole = operatorRole; }
    public Integer getEvidenceVersion() { return evidenceVersion; }
    public void setEvidenceVersion(Integer evidenceVersion) { this.evidenceVersion = evidenceVersion; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
