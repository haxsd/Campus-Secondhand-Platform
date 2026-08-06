package com.campus.trade.dispute.entity;

import java.time.LocalDateTime;

/**
 * 纠纷表实体，与 dispute 表一一对应。
 *
 * <p>数据库里的 evidence 列是 JSON 类型，这里用 {@code evidenceJson} 字符串承载，
 * 由 DisputeService 负责在「图片地址列表」和「JSON 字符串」之间转换。</p>
 */
public class Dispute {

    /** 纠纷主键。 */
    private Long id;

    /** 关联订单，一个订单最多一条纠纷（数据库有唯一约束）。 */
    private Long orderId;

    /** 发起人，买家或卖家。 */
    private Long applicantId;

    /** 被申请人，即交易的另一方。 */
    private Long respondentId;

    /** 处理该纠纷的管理员，未处理时为 null。 */
    private Long handlerId;

    /** 纠纷原因类型：0 货不对板、1 未履约、2 其它。 */
    private Integer reasonType;

    /** 纠纷状态：0 待处理、1 待补材料、2 已驳回、3 维持完成、4 取消交易。 */
    private Integer status;

    /** 发起人填写的问题说明。 */
    private String statement;

    /** 证据图片地址列表，以 JSON 数组字符串存储。 */
    private String evidenceJson;

    /** 当前证据版本，每次追加材料成功后递增。 */
    private Integer evidenceVersion;

    /** 管理员的处理备注。 */
    private String handleNote;

    /** 处理时间；要求补充材料时保持为 null。 */
    private LocalDateTime handledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public Long getRespondentId() {
        return respondentId;
    }

    public void setRespondentId(Long respondentId) {
        this.respondentId = respondentId;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public Integer getReasonType() {
        return reasonType;
    }

    public void setReasonType(Integer reasonType) {
        this.reasonType = reasonType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public Integer getEvidenceVersion() {
        return evidenceVersion;
    }

    public void setEvidenceVersion(Integer evidenceVersion) {
        this.evidenceVersion = evidenceVersion;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
