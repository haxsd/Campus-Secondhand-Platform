package com.campus.trade.review.entity;

import java.time.LocalDateTime;

/** 一笔已完成订单对应的一条买家评价，映射 trade_review 表。 */
public class TradeReview {
    private Long id; private Long orderId; private Long reviewerId; private Long sellerId;
    private Integer rating; private String content; private String tags; private Integer visible; private LocalDateTime createdAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getReviewerId() { return reviewerId; } public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public Long getSellerId() { return sellerId; } public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Integer getRating() { return rating; } public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public String getTags() { return tags; } public void setTags(String tags) { this.tags = tags; }
    public Integer getVisible() { return visible; } public void setVisible(Integer visible) { this.visible = visible; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
