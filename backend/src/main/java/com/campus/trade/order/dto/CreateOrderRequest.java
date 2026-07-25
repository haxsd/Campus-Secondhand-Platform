package com.campus.trade.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 创建线下交易订单的请求参数。 */
public record CreateOrderRequest(

        @NotNull(message = "商品 ID 不能为空")
        @Min(value = 1, message = "商品 ID 不正确")
        Long productId,

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为 1")
        @Max(value = 99999, message = "购买数量不能超过 99999")
        Integer quantity,

        /** 前端日期选择器传入 yyyy-MM-dd HH:mm:ss，明确格式避免 LocalDateTime 解析歧义。 */
        @NotNull(message = "请选择交易时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime tradeTime,

        @NotBlank(message = "请填写交易地点")
        @Size(max = 60, message = "交易地点不能超过 60 个字符")
        String tradePlace,

        @Size(max = 200, message = "备注不能超过 200 个字符")
        String remark,

        /** 同一页面重试必须复用同一个 requestId，数据库唯一索引据此防止重复扣库存。 */
        @NotBlank(message = "请求幂等 ID 不能为空")
        @Size(max = 64, message = "请求幂等 ID 不能超过 64 个字符")
        String requestId
) {
}
