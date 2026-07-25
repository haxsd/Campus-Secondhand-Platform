package com.campus.trade.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品公开列表的一条精简记录。
 *
 * <p>列表不返回描述、图片全集和卖家资料，避免首页一次返回过多数据；
 * 用户点击某条商品后再请求详情接口。</p>
 */
public record ProductListVO(
        Long id,
        String title,
        BigDecimal price,
        String cover,
        String campus,
        Integer itemCondition,
        Integer stock,
        LocalDateTime createdAt
) {
}
