package com.campus.trade.ai.dispute;

import com.campus.trade.dispute.vo.DisputeDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 从管理员聚合详情构建 Agent 输入。
 *
 * <p>显式逐字段组装而不是把 VO 整体序列化，保证后续给管理员 VO 增加手机号等字段时，
 * 不会无意间把隐私数据带进模型请求。</p>
 */
@Component
public class DisputeAgentSnapshotBuilder {
    private final ObjectMapper objectMapper;

    public DisputeAgentSnapshotBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    /** 构建脱敏快照。 */
    public DisputeAgentInputSnapshot build(DisputeDetailVO detail) {
        DisputeDetailVO.OrderSummary order = detail.order();
        DisputeDetailVO.Participant applicant = detail.applicant();
        DisputeDetailVO.Participant respondent = detail.respondent();
        return new DisputeAgentInputSnapshot(
                detail.reasonType(), detail.evidenceVersion(), detail.statement(), detail.evidence(),
                detail.evidenceLogs().stream().map(log -> new DisputeAgentInputSnapshot.EvidenceAddition(
                        log.operatorRole(), log.evidenceVersion(), log.statement(), log.evidence(), log.createdAt()
                )).toList(),
                new DisputeAgentInputSnapshot.Order(
                        order.orderNo(), order.unitPrice(), order.totalAmount(), order.quantity(),
                        order.status(), order.tradeTime(), order.finishedAt()
                ),
                new DisputeAgentInputSnapshot.ProductSnapshotAtOrder(
                        detail.snapshot().getTitle(), detail.snapshot().getDescription(), detail.snapshot().getPrice(),
                        String.valueOf(detail.snapshot().getItemCondition()),
                        parseImages(detail.snapshot().getImagesJson())
                ),
                new DisputeAgentInputSnapshot.Participant(applicant.nickname(), applicant.avatar()),
                new DisputeAgentInputSnapshot.Participant(respondent.nickname(), respondent.avatar()),
                toCredit(detail.applicantCredit()), toCredit(detail.respondentCredit()),
                detail.review() == null ? null : new DisputeAgentInputSnapshot.Review(
                        detail.review().getContent(), detail.review().getRating(), detail.review().getCreatedAt()
                ),
                detail.orderLogs().stream().map(log -> new DisputeAgentInputSnapshot.OrderLog(
                        log.getToStatus(), log.getCreatedAt(), log.getReason()
                )).toList()
        );
    }

    /** 返回用于 verifiedFacts 原文校验的字段索引，只包含可能被模型引用的文本字段。 */
    public Map<String, String> factFields(DisputeAgentInputSnapshot snapshot) {
        return Map.of(
                "applicantStatement", value(snapshot.applicantStatement()),
                "productTitle", value(snapshot.productSnapshotAtOrder().title()),
                "productDescription", value(snapshot.productSnapshotAtOrder().description()),
                "evidence", String.join("\n", snapshot.currentEvidence()),
                "evidenceAdditions", snapshot.evidenceAdditions().stream()
                        .map(addition -> String.join("\n", value(addition.statement()),
                                String.join("\n", addition.evidence() == null ? List.of() : addition.evidence())))
                        .reduce((a, b) -> a + "\n" + b).orElse("")
        );
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private DisputeAgentInputSnapshot.Credit toCredit(com.campus.trade.user.entity.CreditSummary credit) {
        if (credit == null) return null;
        return new DisputeAgentInputSnapshot.Credit(credit.getCreditScore(), credit.getBadReviewCount());
    }
    private String value(String value) { return value == null ? "" : value; }
}
