package com.campus.trade.product.vo;

import java.math.BigDecimal;

/**
 * 商品详情中允许公开的卖家与信用摘要。
 *
 * <p>不包含手机号、学号和账号状态等卖家私有信息。</p>
 */
public record ProductSellerVO(
        Long id,
        String nickname,
        String avatar,
        Integer creditScore,
        Integer dealCount,
        BigDecimal avgRating,
        BigDecimal goodReviewRate
) {
}
