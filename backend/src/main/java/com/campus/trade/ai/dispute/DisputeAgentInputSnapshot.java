package com.campus.trade.ai.dispute;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠纷 Agent 的脱敏输入快照。
 *
 * <p>该类型只保留判案有用字段，刻意不包含手机号、学号、handlerId 等个人或内部字段。</p>
 */
public record DisputeAgentInputSnapshot(
        Integer reasonType,
        Integer evidenceVersion,
        String applicantStatement,
        List<String> currentEvidence,
        List<EvidenceAddition> evidenceAdditions,
        Order order,
        ProductSnapshotAtOrder productSnapshotAtOrder,
        Participant applicant,
        Participant respondent,
        Credit applicantCredit,
        Credit respondentCredit,
        Review review,
        List<OrderLog> orderTimeline
) {
    public record EvidenceAddition(Integer operatorRole, Integer evidenceVersion, String statement, List<String> evidence, LocalDateTime createdAt) { }
    public record Order(String orderNo, BigDecimal unitPrice, BigDecimal totalAmount, Integer quantity, Integer status, LocalDateTime tradeTime, LocalDateTime finishedAt) { }
    public record ProductSnapshotAtOrder(String title, String description, BigDecimal price, String condition, List<String> images) { }
    public record Participant(String nickname, String avatar) { }
    public record Credit(Integer score, Integer badReviewCount) { }
    public record Review(String content, Integer rating, LocalDateTime createdAt) { }
    public record OrderLog(Integer status, LocalDateTime createdAt, String note) { }
}
