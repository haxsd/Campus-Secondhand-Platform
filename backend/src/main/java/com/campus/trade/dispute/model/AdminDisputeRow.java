package com.campus.trade.dispute.model;

import java.time.LocalDateTime;

/**
 * 管理端纠纷列表的一行数据库读模型。
 *
 * <p>该对象由一条 JOIN SQL 直接映射，避免 Service 对每条纠纷分别查询订单、商品和两名用户。
 * evidenceJson 保持为数据库 JSON 文本，Service 在返回接口前再统一转换为字符串列表，
 * 从而保持既有 API 响应结构不变。</p>
 */
public class AdminDisputeRow {

    private Long id;
    private Long orderId;
    private Integer reasonType;
    private String statement;
    private String evidenceJson;
    private Integer status;
    private String orderNo;
    private String productTitle;
    private String buyerName;
    private String sellerName;
    private Integer orderStatus;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getReasonType() { return reasonType; }
    public void setReasonType(Integer reasonType) { this.reasonType = reasonType; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
