package com.campus.trade.dispute.vo;

import com.campus.trade.order.entity.ProductSnapshot;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.review.entity.TradeReview;
import com.campus.trade.user.entity.CreditSummary;

import java.time.LocalDateTime;
import java.util.List;

public record DisputeDetailVO(
        Long id, Long orderId, Long applicantId, Long respondentId, Integer reasonType,
        String statement, List<String> evidence, Integer status, Integer evidenceVersion,
        Long handlerId, String handleNote, LocalDateTime handledAt, LocalDateTime createdAt,
        OrderSummary order, ProductSnapshot snapshot, Participant applicant, Participant respondent,
        CreditSummary applicantCredit, CreditSummary respondentCredit, TradeReview review,
        List<TradeOrderLog> orderLogs, List<EvidenceLogVO> evidenceLogs
) {
    public record OrderSummary(Long id, String orderNo, Long buyerId, Long sellerId, Long productId,
                               Integer quantity, java.math.BigDecimal unitPrice, java.math.BigDecimal totalAmount,
                               Integer status, Integer statusBeforeDispute, String remark,
                               LocalDateTime tradeTime, LocalDateTime finishedAt) {}

    public record Participant(Long id, String nickname, String avatar, String campus) {}

    public record EvidenceLogVO(Long id, Long operatorId, Integer operatorRole, Integer evidenceVersion,
                                String statement, List<String> evidence, LocalDateTime createdAt) {}
}
