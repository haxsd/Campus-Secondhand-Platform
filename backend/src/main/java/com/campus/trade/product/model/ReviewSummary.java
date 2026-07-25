package com.campus.trade.product.model;

import java.time.LocalDateTime;

/**
 * 商品详情中展示的单条评价读模型，对应 trade_review 的公开字段。
 */
public class ReviewSummary {

    private Integer rating;
    private String content;
    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
