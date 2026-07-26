package com.campus.trade.dispute.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端纠纷列表的一行。
 *
 * <p>除了纠纷本身的字段外，还冗余了订单号、商品标题和买卖双方昵称，
 * 这样管理员在列表页就能判断情况，不必再逐条点开查订单。</p>
 *
 * @param evidence    证据图片地址列表，由 dispute.evidence 这一 JSON 列解析而来
 * @param orderStatus 订单当前状态，纠纷处理中通常为 5
 */
public record AdminDisputeVO(
        Long id,
        Long orderId,
        Integer reasonType,
        String statement,
        List<String> evidence,
        Integer status,
        String orderNo,
        String productTitle,
        String buyerName,
        String sellerName,
        Integer orderStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
) {
}
