package com.campus.trade.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 一条订单状态流转记录。 */
public record OrderLogVO(
        Integer fromStatus,
        Integer toStatus,
        Integer operatorType,
        String reason,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
) {
}
