package com.campus.trade.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单详情以及当前访问者可执行操作的标记。 */
public record OrderDetailVO(
        Long id,
        String orderNo,
        Integer status,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime tradeTime,
        String tradePlace,
        String remark,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime confirmDeadline,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime finishedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        OrderPartyVO buyer,
        OrderPartyVO seller,
        OrderSnapshotVO snapshot,
        List<OrderLogVO> logs,
        boolean canConfirm,
        boolean canCancel,
        boolean canComplete,
        /** 评价、纠纷在后续模块实现前统一返回 false，前端不会展示尚未接入的操作。 */
        boolean canReview,
        boolean canDispute
) {
}
