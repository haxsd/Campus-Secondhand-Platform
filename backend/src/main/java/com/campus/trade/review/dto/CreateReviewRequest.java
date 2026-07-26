package com.campus.trade.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 提交评价的请求体。
 *
 * <p>评价人和被评价人都不由前端传入：评价人取自 token，被评价人取自订单上的卖家。</p>
 *
 * @param rating 评分 1~5，4 分及以上计为好评，2 分及以下计为差评
 * @param tags   评价标签，前端把多选结果拼成逗号分隔的字符串提交
 */
public record CreateReviewRequest(
        @NotNull @Min(1) Long orderId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 500) String content,
        @Size(max = 200) String tags
) {
}
