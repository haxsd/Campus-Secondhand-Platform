package com.campus.trade.order.vo;

import java.math.BigDecimal;
import java.util.List;

/** 下单时刻冻结的商品展示数据。 */
public record OrderSnapshotVO(
        String title,
        String description,
        BigDecimal price,
        Integer itemCondition,
        String campus,
        String tradePlace,
        List<String> images
) {
}
