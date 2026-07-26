package com.campus.trade.review.entity;

import java.time.LocalDateTime;

/** 评价表实体，与 trade_review 表一一对应。 */
public class TradeReview {

    private Long id;

    /** 被评价的订单，一单一评（数据库有唯一约束）。 */
    private Long orderId;

    /** 评价人，只能是订单买家。 */
    private Long reviewerId;

    /** 被评价人，取订单卖家；冗余在这里是为了按卖家聚合评分时不必再关联订单表。 */
    private Long sellerId;

    /** 评分 1~5。 */
    private Integer rating;

    private String content;

    /** 评价标签，多个标签以逗号分隔存成一个字符串。 */
    private String tags;

    /** 是否展示：1 可见、0 已隐藏；重算卖家信用时只统计可见评价。 */
    private Integer visible;

    private LocalDateTime createdAt;

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

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
