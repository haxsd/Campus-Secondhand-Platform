package com.campus.trade.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单商品快照实体，对应 product_snapshot。
 *
 * <p>imagesJson 保留数据库 JSON 原文；在 Service 层转换为 List&lt;String&gt; 后再返回前端，
 * 使数据库格式细节不会泄漏到 Controller。</p>
 */
public class ProductSnapshot {

    private Long id;
    private Long orderId;
    private Long productId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer itemCondition;
    private String campus;
    private String tradePlace;
    private String imagesJson;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getItemCondition() { return itemCondition; }
    public void setItemCondition(Integer itemCondition) { this.itemCondition = itemCondition; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getTradePlace() { return tradePlace; }
    public void setTradePlace(String tradePlace) { this.tradePlace = tradePlace; }
    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
