package com.campus.trade.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员待审核列表的一条商品记录。
 *
 * <p>管理员需要看到卖家昵称、分类和图片来做审核，但仍不应看到卖家手机号、学号等隐私数据。</p>
 */
public record PendingProductVO(
        Long id,
        String title,
        BigDecimal price,
        Integer stock,
        Integer itemCondition,
        String categoryName,
        String cover,
        List<String> images,
        ProductSellerVO seller,
        LocalDateTime createdAt
) {
}
