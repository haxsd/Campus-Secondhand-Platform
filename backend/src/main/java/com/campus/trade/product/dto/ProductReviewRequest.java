package com.campus.trade.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员审核商品的请求参数。
 *
 * @param pass   true 表示通过并上架；false 表示驳回
 * @param reason 驳回原因；通过时可为空，驳回时由 Service 继续校验不能为空
 */
public record ProductReviewRequest(
        @NotNull(message = "请选择审核结果")
        Boolean pass,

        @Size(max = 500, message = "审核原因不能超过 500 个字符")
        String reason
) {
}
