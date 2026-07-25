package com.campus.trade.product.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品公开详情响应。
 *
 * <p>该对象由商品主表、分类、卖家信用摘要、图片和评价共同组合而成，
 * 因此它不是任意一张数据库表的实体。</p>
 */
public record ProductDetailVO(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Integer stock,
        Integer itemCondition,
        String campus,
        String tradePlace,
        Integer status,
        Long categoryId,
        String categoryName,
        Integer viewCount,
        List<String> images,
        ProductSellerVO seller,
        List<RecentReviewVO> recentReviews
) {
}
