package com.campus.trade.dispute.vo;

import com.campus.trade.order.entity.ProductSnapshot;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.review.entity.TradeReview;
import com.campus.trade.user.entity.CreditSummary;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠纷详情聚合模型。关联数据由 Service 一次组装，供管理员详情页和后续 Agent 输入快照复用。
 */
public record DisputeDetailVO(
        Long id, Long orderId, Long applicantId, Long respondentId, Integer reasonType,
        String statement, List<String> evidence, Integer status, Integer evidenceVersion,
        Long handlerId, String handleNote, @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime handledAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
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
