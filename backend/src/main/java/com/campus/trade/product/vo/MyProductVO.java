package com.campus.trade.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 卖家“我的商品”列表使用的完整管理数据。
 *
 * <p>它包含版本号和图片列表，前端进入编辑页时可以直接回填表单；
 * 这些字段不会出现在公开商品列表中。</p>
 */
public record MyProductVO(
        Long id,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        Integer stock,
        Integer itemCondition,
        String campus,
        String tradePlace,
        Integer status,
        Integer version,
        String cover,
        List<String> images,
        String rejectReason,
        LocalDateTime createdAt
) {
}
