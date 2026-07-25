package com.campus.trade.history.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 浏览记录页的一条商品卡片数据。
 *
 * <p>字段与首页 ProductCard 所需字段保持一致，并额外提供 lastViewTime，
 * 前端无需为浏览记录页面再编写一套不同的卡片组件。</p>
 */
public record BrowseHistoryVO(
        Long id,
        Long productId,
        String title,
        String cover,
        BigDecimal price,
        Integer itemCondition,
        String campus,
        LocalDateTime lastViewTime
) {
}
