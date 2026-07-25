package com.campus.trade.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 卖家调整在售商品库存的请求参数。
 *
 * @param delta 增减量；正数增加，负数减少，0 会在 Service 中拒绝
 */
public record StockAdjustRequest(
        @NotNull(message = "请输入库存增减数量")
        @Min(value = -99999, message = "库存调整数量过小")
        @Max(value = 99999, message = "库存调整数量过大")
        Integer delta
) {
}
