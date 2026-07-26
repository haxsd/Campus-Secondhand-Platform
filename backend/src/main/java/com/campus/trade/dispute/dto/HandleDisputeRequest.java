package com.campus.trade.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员裁决纠纷的请求体。
 *
 * <p>{@code action} 用 {@link Pattern} 限定取值，非法动作在进入 Service 之前就会被拦成 400，
 * Service 里的 switch 只需要处理这四种情况。</p>
 *
 * @param action  REJECT 驳回 / KEEP_COMPLETED 维持完成 / CANCEL_TRADE 取消交易 / NEED_MORE 要求补充材料
 * @param restock 仅在 CANCEL_TRADE 时有意义：是否把订单占用的库存退还给商品
 */
public record HandleDisputeRequest(
        @NotBlank @Pattern(regexp = "REJECT|KEEP_COMPLETED|CANCEL_TRADE|NEED_MORE") String action,
        Boolean restock,
        @Size(max = 1000) String note
) {
}
