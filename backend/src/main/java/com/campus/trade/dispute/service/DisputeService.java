package com.campus.trade.dispute.service;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.CursorPageResult;
import com.campus.trade.dispute.dto.AppendDisputeEvidenceRequest;
import com.campus.trade.dispute.dto.CreateDisputeRequest;
import com.campus.trade.dispute.dto.HandleDisputeRequest;
import com.campus.trade.dispute.entity.Dispute;
import com.campus.trade.dispute.entity.DisputeEvidenceLog;
import com.campus.trade.dispute.mapper.DisputeEvidenceLogMapper;
import com.campus.trade.dispute.mapper.DisputeMapper;
import com.campus.trade.dispute.model.AdminDisputeRow;
import com.campus.trade.dispute.model.DisputeRules;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import com.campus.trade.order.entity.ProductSnapshot;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.SellerDetailCacheInvalidator;
import com.campus.trade.review.entity.TradeReview;
import com.campus.trade.review.mapper.ReviewMapper;
import com.campus.trade.user.entity.CreditSummary;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DisputeService {
    private static final int PENDING = 0;
    private static final int NEED_MORE = 1;
    private static final int REJECTED = 2;
    private static final int KEEP_COMPLETED = 3;
    private static final int CANCEL_TRADE = 4;
    private static final int MAX_EVIDENCE = 5;
    private static final int BUYER = 0;
    private static final int SELLER = 1;
    private static final int ADMIN = 3;

    private final DisputeMapper disputes;
    private final DisputeEvidenceLogMapper evidenceLogs;
    private final OrderMapper orders;
    private final ProductMapper products;
    private final UserMapper users;
    private final ReviewMapper reviews;
    private final SellerDetailCacheInvalidator cacheInvalidator;
    private final ObjectMapper json;

    @Autowired
    public DisputeService(DisputeMapper disputes, DisputeEvidenceLogMapper evidenceLogs, OrderMapper orders,
                          ProductMapper products, UserMapper users, ReviewMapper reviews,
                          SellerDetailCacheInvalidator cacheInvalidator, ObjectMapper json) {
        this.disputes = disputes;
        this.evidenceLogs = evidenceLogs;
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.reviews = reviews;
        this.cacheInvalidator = cacheInvalidator;
        this.json = json;
    }

    public DisputeService(DisputeMapper disputes, OrderMapper orders, ProductMapper products, UserMapper users, SellerDetailCacheInvalidator cacheInvalidator, ObjectMapper json) {
        this(disputes, null, orders, products, users, null, cacheInvalidator, json);
    }

    @Transactional
    public DisputeCreatedVO create(CreateDisputeRequest request) {
        Long userId = UserContext.requireCurrentUser().userId();
        TradeOrder order = orders.selectById(request.orderId()).orElseThrow(() -> error(ErrorCode.NOT_FOUND, "order not found"));
        boolean buyer = Objects.equals(userId, order.getBuyerId());
        boolean seller = Objects.equals(userId, order.getSellerId());
        if (!buyer && !seller) throw error(ErrorCode.FORBIDDEN, "participant required");
        int fromStatus = order.getStatus();
        if (fromStatus != OrderStatus.CONFIRMED.getCode() && fromStatus != OrderStatus.COMPLETED.getCode()) {
            throw conflict("order status does not allow dispute");
        }
        if (fromStatus == OrderStatus.COMPLETED.getCode() && !DisputeRules.withinAfterSaleWindow(order.getFinishedAt())) {
            throw conflict("\u8BA2\u5355\u5B8C\u6210\u5DF2\u8D85\u8FC7 7 \u5929\uFF0C\u4E0D\u80FD\u518D\u53D1\u8D77\u7EA0\u7EB7");
        }
        if (disputes.existsByOrderId(order.getId())) throw conflict("dispute already exists");
        Dispute dispute = new Dispute();
        dispute.setOrderId(order.getId());
        dispute.setApplicantId(userId);
        dispute.setRespondentId(buyer ? order.getSellerId() : order.getBuyerId());
        dispute.setReasonType(request.reasonType());
        dispute.setStatement(request.statement().trim());
        dispute.setEvidenceJson(writeEvidence(request.evidence()));
        dispute.setEvidenceVersion(1);
        dispute.setStatus(PENDING);
        try { disputes.insert(dispute); } catch (DuplicateKeyException e) { throw conflict("dispute already exists"); }
        if (orders.enterDispute(order.getId(), userId, fromStatus) == 0) throw conflict("order changed, retry");
        insertOrderLog(order.getId(), fromStatus, OrderStatus.IN_DISPUTE.getCode(), buyer ? BUYER : SELLER, userId, dispute.getStatement());
        return new DisputeCreatedVO(dispute.getId());
    }

    public DisputeDetailVO getForParticipant(Long id) {
        Long userId = UserContext.requireCurrentUser().userId();
        Dispute dispute = findDispute(id);
        if (participantRole(dispute, userId) < 0) throw error(ErrorCode.FORBIDDEN, "participant required");
        return buildDetail(dispute);
    }

    public DisputeDetailVO getForOrder(Long orderId) {
        Long userId = UserContext.requireCurrentUser().userId();
        Dispute dispute = disputes.selectByOrderId(orderId).orElseThrow(() -> error(ErrorCode.NOT_FOUND, "dispute not found"));
        if (participantRole(dispute, userId) < 0) throw error(ErrorCode.FORBIDDEN, "participant required");
        return buildDetail(dispute);
    }

    @Transactional
    public void appendEvidence(Long id, AppendDisputeEvidenceRequest request) {
        Long userId = UserContext.requireCurrentUser().userId();
        Dispute dispute = findDispute(id);
        int role = participantRole(dispute, userId);
        if (role < 0) throw error(ErrorCode.FORBIDDEN, "participant required");
        if (!Objects.equals(dispute.getStatus(), NEED_MORE)) throw conflict("dispute is not waiting for evidence");
        List<String> additions = request.evidence() == null ? List.of() : request.evidence();
        String statement = request.statement() == null ? null : request.statement().trim();
        if ((statement == null || statement.isBlank()) && additions.isEmpty()) throw error(ErrorCode.BAD_REQUEST, "supplement is empty");
        List<String> merged = new ArrayList<>(readEvidence(dispute.getEvidenceJson()));
        merged.addAll(additions);
        if (merged.size() > MAX_EVIDENCE) throw error(ErrorCode.BAD_REQUEST, "at most 5 evidence images");
        int version = dispute.getEvidenceVersion() == null ? 1 : dispute.getEvidenceVersion();
        if (disputes.appendEvidence(id, version, writeEvidence(merged)) == 0) throw conflict("evidence changed, refresh");
        DisputeEvidenceLog log = new DisputeEvidenceLog();
        log.setDisputeId(id); log.setOperatorId(userId); log.setOperatorRole(role);
        log.setEvidenceVersion(version + 1); log.setStatement(statement); log.setEvidenceJson(writeEvidence(additions));
        evidenceLogs.insert(log);
    }

    @Transactional
    public void handle(Long id, HandleDisputeRequest request) {
        Long adminId = UserContext.requireCurrentUser().userId();
        Dispute dispute = findDispute(id);
        TradeOrder order = orders.selectById(dispute.getOrderId()).orElseThrow(() -> error(ErrorCode.NOT_FOUND, "order not found"));
        String note = trimToNull(request.note());
        switch (request.action()) {
            case "NEED_MORE" -> updateStatus(dispute, NEED_MORE, adminId, note, request.evidenceVersion());
            case "REJECT" -> { updateStatus(dispute, REJECTED, adminId, note, request.evidenceVersion()); restoreOrder(order, adminId, note); }
            case "KEEP_COMPLETED" -> { updateStatus(dispute, KEEP_COMPLETED, adminId, note, request.evidenceVersion()); completeOrder(order, adminId, note); }
            case "CANCEL_TRADE" -> { updateStatus(dispute, CANCEL_TRADE, adminId, note, request.evidenceVersion()); cancelOrder(order, adminId, note, request.restock()); }
            default -> throw error(ErrorCode.BAD_REQUEST, "unknown action");
        }
    }

    public DisputeDetailVO adminDetail(Long id) { return buildDetail(findDispute(id)); }

    public CursorPageResult<AdminDisputeVO> list(Integer status, LocalDateTime cursorCreatedAt, Long cursorId, int pageSize) {
        List<AdminDisputeRow> rows = disputes.selectAdminCursorPage(status, cursorCreatedAt, cursorId, pageSize + 1);
        boolean next = rows.size() > pageSize;
        List<AdminDisputeRow> current = next ? rows.subList(0, pageSize) : rows;
        List<AdminDisputeVO> result = current.stream().map(this::toAdmin).toList();
        if (!next) return new CursorPageResult<>(result, false, null, null);
        AdminDisputeRow last = current.get(current.size() - 1);
        return new CursorPageResult<>(result, true, last.getCreatedAt(), last.getId());
    }

    private void restoreOrder(TradeOrder order, Long adminId, String note) {
        if (orders.restoreFromDispute(order.getId(), order.getStatusBeforeDispute()) == 0) throw conflict("order changed");
        insertOrderLog(order.getId(), OrderStatus.IN_DISPUTE.getCode(), order.getStatusBeforeDispute(), ADMIN, adminId, note);
    }
    private void completeOrder(TradeOrder order, Long adminId, String note) {
        if (orders.completeFromDispute(order.getId()) == 0) throw conflict("order changed");
        if (order.getStatusBeforeDispute() == OrderStatus.CONFIRMED.getCode()) users.incrementDealCount(order.getSellerId());
        cacheInvalidator.invalidateAfterCommit(order.getSellerId());
        insertOrderLog(order.getId(), OrderStatus.IN_DISPUTE.getCode(), OrderStatus.COMPLETED.getCode(), ADMIN, adminId, note);
    }
    private void cancelOrder(TradeOrder order, Long adminId, String note, Boolean restock) {
        if (orders.cancelFromDispute(order.getId()) == 0) throw conflict("order changed");
        if (Boolean.TRUE.equals(restock)) products.restoreStockForCancelledOrder(order.getProductId(), order.getQuantity());
        cacheInvalidator.invalidateAfterCommit(order.getSellerId());
        insertOrderLog(order.getId(), OrderStatus.IN_DISPUTE.getCode(), OrderStatus.CANCELLED.getCode(), ADMIN, adminId, note);
    }
    private void updateStatus(Dispute dispute, int status, Long adminId, String note, int version) {
        if (disputes.updateHandled(dispute.getId(), status, adminId, note, version) == 0) throw conflict("dispute changed, refresh");
    }

    private DisputeDetailVO buildDetail(Dispute dispute) {
        TradeOrder order = orders.selectById(dispute.getOrderId()).orElse(null);
        User applicant = users.selectById(dispute.getApplicantId()).orElse(null);
        User respondent = users.selectById(dispute.getRespondentId()).orElse(null);
        CreditSummary applicantCredit = users.selectCreditSummary(dispute.getApplicantId()).orElse(null);
        CreditSummary respondentCredit = users.selectCreditSummary(dispute.getRespondentId()).orElse(null);
        ProductSnapshot snapshot = order == null ? null : orders.selectSnapshotByOrderId(order.getId()).orElse(null);
        TradeReview review = order == null ? null : reviews.selectByOrderId(order.getId());
        List<TradeOrderLog> logs = order == null ? List.of() : orders.selectLogsByOrderId(order.getId());
        List<DisputeDetailVO.EvidenceLogVO> evidence = evidenceLogs.selectByDisputeId(dispute.getId()).stream().map(log ->
                new DisputeDetailVO.EvidenceLogVO(log.getId(), log.getOperatorId(), log.getOperatorRole(), log.getEvidenceVersion(), log.getStatement(), readEvidence(log.getEvidenceJson()), log.getCreatedAt())).toList();
        DisputeDetailVO.OrderSummary summary = order == null ? null : new DisputeDetailVO.OrderSummary(order.getId(), order.getOrderNo(), order.getBuyerId(), order.getSellerId(), order.getProductId(), order.getQuantity(), order.getUnitPrice(), order.getTotalAmount(), order.getStatus(), order.getStatusBeforeDispute(), order.getRemark(), order.getTradeTime(), order.getFinishedAt());
        return new DisputeDetailVO(dispute.getId(), dispute.getOrderId(), dispute.getApplicantId(), dispute.getRespondentId(), dispute.getReasonType(), dispute.getStatement(), readEvidence(dispute.getEvidenceJson()), dispute.getStatus(), dispute.getEvidenceVersion(), dispute.getHandlerId(), dispute.getHandleNote(), dispute.getHandledAt(), dispute.getCreatedAt(), summary, snapshot, participant(applicant), participant(respondent), applicantCredit, respondentCredit, review, logs, evidence);
    }
    private DisputeDetailVO.Participant participant(User user) { return user == null ? null : new DisputeDetailVO.Participant(user.getId(), user.getNickname(), user.getAvatar(), user.getCampus()); }
    private AdminDisputeVO toAdmin(AdminDisputeRow row) { return new AdminDisputeVO(row.getId(), row.getOrderId(), row.getReasonType(), row.getStatement(), readEvidence(row.getEvidenceJson()), row.getStatus(), row.getEvidenceVersion(), row.getOrderNo(), row.getProductTitle(), row.getBuyerName(), row.getSellerName(), row.getOrderStatus(), row.getCreatedAt()); }
    private int participantRole(Dispute dispute, Long userId) { if (Objects.equals(dispute.getApplicantId(), userId)) return 0; if (Objects.equals(dispute.getRespondentId(), userId)) return 1; return -1; }
    private void insertOrderLog(Long orderId, int from, int to, int type, Long userId, String reason) { TradeOrderLog log = new TradeOrderLog(); log.setOrderId(orderId); log.setFromStatus(from); log.setToStatus(to); log.setOperatorType(type); log.setOperatorId(userId); log.setReason(reason); orders.insertLog(log); }
    private Dispute findDispute(Long id) { return disputes.selectById(id).orElseThrow(() -> error(ErrorCode.NOT_FOUND, "dispute not found")); }
    private String writeEvidence(List<String> value) { try { return json.writeValueAsString(value == null ? List.of() : value); } catch (Exception e) { throw new IllegalStateException(e); } }
    private List<String> readEvidence(String value) { try { return value == null ? List.of() : json.readValue(value, new TypeReference<List<String>>() {}); } catch (Exception e) { return List.of(); } }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private BizException conflict(String message) { return error(ErrorCode.CONFLICT, message); }
    private BizException error(ErrorCode code, String message) { return new BizException(code, message); }
}
