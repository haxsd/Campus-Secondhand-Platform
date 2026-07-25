package com.campus.trade.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 创建订单成功后返回的最小数据，前端用 id 跳转至订单详情。 */
public record OrderCreatedVO(
        Long id,
        String orderNo,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime confirmDeadline
) {
}
