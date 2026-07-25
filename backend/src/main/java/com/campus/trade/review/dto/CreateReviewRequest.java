package com.campus.trade.review.dto;
import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min; import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Size;
/** 已完成订单的买家评价请求。卖家和评价人均由订单、登录态推导。 */
public record CreateReviewRequest(@NotNull @Min(1) Long orderId, @NotNull @Min(1) @Max(5) Integer rating,
                                  @Size(max = 500) String content, @Size(max = 200) String tags) { }
