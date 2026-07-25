package com.campus.trade.order.dto;

import jakarta.validation.constraints.Size;

/** 取消订单请求；原因可选，但限制长度以保护状态日志字段。 */
public record CancelOrderRequest(
        @Size(max = 200, message = "取消原因不能超过 200 个字符")
        String reason
) {
}
