package com.campus.trade.dispute.vo;

import com.campus.trade.order.entity.ProductSnapshot;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当事人可见的纠纷详情。信用摘要、评价、订单日志和处理管理员信息仅属于管理端。
 */
public record ParticipantDisputeDetailVO(
        Long id, Long orderId, Long applicantId, Long respondentId, Integer reasonType,
        String statement, List<String> evidence, Integer status, Integer evidenceVersion,
        String handleNote, @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime handledAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        OrderSummary order, ProductSnapshot snapshot, Participant applicant, Participant respondent,
        List<EvidenceLogVO> evidenceLogs
) {
    public record OrderSummary(Long id, String orderNo, Integer quantity,
                               java.math.BigDecimal unitPrice, java.math.BigDecimal totalAmount,
                               Integer status, LocalDateTime tradeTime, LocalDateTime finishedAt) {}

    public record Participant(Long id, String nickname, String avatar, String campus) {}

    public record EvidenceLogVO(Long id, Long operatorId, Integer operatorRole, Integer evidenceVersion,
                                String statement, List<String> evidence, LocalDateTime createdAt) {}
}
