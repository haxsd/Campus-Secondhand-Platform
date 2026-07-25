package com.campus.trade.product.vo;

import java.time.LocalDateTime;

/**
 * 商品详情展示的卖家最近评价。
 *
 * @param rating    星级，1~5
 * @param content   买家评价内容
 * @param createdAt 评价创建时间
 */
public record RecentReviewVO(Integer rating, String content, LocalDateTime createdAt) {
}
