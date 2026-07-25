package com.campus.trade.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单主表实体，对应 trade_order。
 *
 * <p>商品标题、图片等可变展示数据不放在订单主表中，而由 product_snapshot 保存下单瞬间的副本；
 * 这样卖家以后编辑商品，不会影响历史订单的展示。</p>
 */
public class TradeOrder {

    private Long id;
    private String orderNo;
    private String requestId;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private LocalDateTime tradeTime;
    private String tradePlace;
    private String remark;
    private Integer status;
    private Integer statusBeforeDispute;
    private LocalDateTime confirmDeadline;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getTradeTime() { return tradeTime; }
    public void setTradeTime(LocalDateTime tradeTime) { this.tradeTime = tradeTime; }
    public String getTradePlace() { return tradePlace; }
    public void setTradePlace(String tradePlace) { this.tradePlace = tradePlace; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getStatusBeforeDispute() { return statusBeforeDispute; }
    public void setStatusBeforeDispute(Integer statusBeforeDispute) { this.statusBeforeDispute = statusBeforeDispute; }
    public LocalDateTime getConfirmDeadline() { return confirmDeadline; }
    public void setConfirmDeadline(LocalDateTime confirmDeadline) { this.confirmDeadline = confirmDeadline; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
