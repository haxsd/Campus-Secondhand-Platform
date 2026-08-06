package com.campus.trade.dispute.model;

import java.time.LocalDateTime;

public class AdminDisputeRow {
    private Long id; private Long orderId; private Integer reasonType; private String statement;
    private String evidenceJson; private Integer status; private Integer evidenceVersion;
    private String orderNo; private String productTitle; private String buyerName; private String sellerName;
    private Integer orderStatus; private LocalDateTime createdAt;
    public Long getId() { return id; } public void setId(Long v) { id=v; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long v) { orderId=v; }
    public Integer getReasonType() { return reasonType; } public void setReasonType(Integer v) { reasonType=v; }
    public String getStatement() { return statement; } public void setStatement(String v) { statement=v; }
    public String getEvidenceJson() { return evidenceJson; } public void setEvidenceJson(String v) { evidenceJson=v; }
    public Integer getStatus() { return status; } public void setStatus(Integer v) { status=v; }
    public Integer getEvidenceVersion() { return evidenceVersion; } public void setEvidenceVersion(Integer v) { evidenceVersion=v; }
    public String getOrderNo() { return orderNo; } public void setOrderNo(String v) { orderNo=v; }
    public String getProductTitle() { return productTitle; } public void setProductTitle(String v) { productTitle=v; }
    public String getBuyerName() { return buyerName; } public void setBuyerName(String v) { buyerName=v; }
    public String getSellerName() { return sellerName; } public void setSellerName(String v) { sellerName=v; }
    public Integer getOrderStatus() { return orderStatus; } public void setOrderStatus(Integer v) { orderStatus=v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt=v; }
}
