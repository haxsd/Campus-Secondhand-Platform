package com.campus.trade.user.entity;

import java.math.BigDecimal;

public class CreditSummary {
    private Long userId;
    private Integer creditScore;
    private Integer dealCount;
    private Integer reviewCount;
    private BigDecimal avgRating;
    private BigDecimal goodReviewRate;
    private Integer badReviewCount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
    public Integer getDealCount() { return dealCount; }
    public void setDealCount(Integer dealCount) { this.dealCount = dealCount; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
    public BigDecimal getGoodReviewRate() { return goodReviewRate; }
    public void setGoodReviewRate(BigDecimal goodReviewRate) { this.goodReviewRate = goodReviewRate; }
    public Integer getBadReviewCount() { return badReviewCount; }
    public void setBadReviewCount(Integer badReviewCount) { this.badReviewCount = badReviewCount; }
}
