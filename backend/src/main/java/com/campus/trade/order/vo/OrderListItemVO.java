package com.campus.trade.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** “我的订单”列表的一项，字段覆盖当前前端列表页所需展示内容。 */
public record OrderListItemVO(
        Long id,
        String orderNo,
        Integer status,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        OrderPartyVO buyer,
        OrderPartyVO seller,
        OrderSnapshotVO snapshot
) {
}
