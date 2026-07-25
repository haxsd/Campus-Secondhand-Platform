package com.campus.trade.product.model;

import java.math.BigDecimal;

/**
 * 商品查询时连接 user 与 user_credit_summary 得到的卖家公开数据。
 *
 * <p>这是 Mapper 的读模型，不直接暴露给前端；Service 会把它转换为 ProductSellerVO。</p>
 */
public class SellerSummary {

    private Long id;
    private String nickname;
    private String avatar;
    private Integer creditScore;
    private Integer dealCount;
    private BigDecimal avgRating;
    private BigDecimal goodReviewRate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public Integer getDealCount() {
        return dealCount;
    }

    public void setDealCount(Integer dealCount) {
        this.dealCount = dealCount;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public BigDecimal getGoodReviewRate() {
        return goodReviewRate;
    }

    public void setGoodReviewRate(BigDecimal goodReviewRate) {
        this.goodReviewRate = goodReviewRate;
    }
}
