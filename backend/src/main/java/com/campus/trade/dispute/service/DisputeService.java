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
import com.campus.trade.dispute.model.DisputeRules;
import com.campus.trade.dispute.model.AdminDisputeRow;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import com.campus.trade.dispute.vo.ParticipantDisputeDetailVO;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 纠纷业务：交易双方发起纠纷，管理员裁决。
 *
 * <p>纠纷的本质是把订单临时“冻结”到 {@code status=5 纠纷中}，等管理员给出结论后再决定订单的最终去向。
 * 为了让驳回纠纷时能把订单还原回冻结前的样子，订单表上有一列 {@code status_before_dispute}，
 * 在进入纠纷时记录原状态，裁决时再读出来使用。</p>
 *
 * <p>并发保护同样采用全项目统一的写法：所有状态变更都是“带条件的 UPDATE + 判断影响行数”，
 * 两个管理员同时点处理时只有一个能成功，另一个会收到 409 冲突提示。</p>
 */
@Service
public class DisputeService {

    /** 纠纷状态：0 待处理、1 待补材料、2 已驳回、3 维持完成、4 取消交易。 */
    private static final int DISPUTE_PENDING = 0;
    private static final int DISPUTE_NEED_MORE = 1;
    private static final int DISPUTE_REJECTED = 2;
    private static final int DISPUTE_KEEP_COMPLETED = 3;
    private static final int DISPUTE_CANCEL_TRADE = 4;

    /** 订单状态日志里的操作者类型：0 买家、1 卖家、2 系统、3 管理员。 */
    private static final int OPERATOR_BUYER = 0;
    private static final int OPERATOR_SELLER = 1;
    private static final int OPERATOR_ADMIN = 3;

    /** 单次追加最多 5 张，避免一次请求过大；历史累计最多 15 张，给当事人保留多轮补充空间。 */
    private static final int MAX_EVIDENCE_PER_APPEND = 5;
    private static final int MAX_EVIDENCE_TOTAL = 15;

    private final DisputeMapper disputes;
    private final DisputeEvidenceLogMapper evidenceLogs;
    private final OrderMapper orders;
    private final ProductMapper products;
    private final UserMapper users;
    private final ReviewMapper reviews;
    private final SellerDetailCacheInvalidator cacheInvalidator;
    private final ObjectMapper json;

    /** 详情查询、证据流水和管理员聚合都依赖完整的数据访问对象；单构造器由 Spring 自动注入。 */
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

    /**
     * 买家或卖家发起纠纷：写入纠纷记录，并把订单冻结为“纠纷中”。
     *
     * <p>三道校验依次是：必须是这笔交易的当事人、订单必须处于已确认或已完成、同一订单只能有一条纠纷。
     * 最后一条除了先查一次以外，还依赖 dispute 表上 order_id 的唯一约束兜底，
     * 因为“先查再插”在并发下并不可靠。</p>
     */
    @Transactional
    public DisputeCreatedVO create(CreateDisputeRequest request) {
        Long currentUserId = UserContext.requireCurrentUser().userId();

        TradeOrder order = orders.selectById(request.orderId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));

        boolean isBuyer = Objects.equals(currentUserId, order.getBuyerId());
        boolean isSeller = Objects.equals(currentUserId, order.getSellerId());
        if (!isBuyer && !isSeller) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有交易双方可以发起纠纷");
        }

        int orderStatus = order.getStatus();
        if (orderStatus != OrderStatus.CONFIRMED.getCode() && orderStatus != OrderStatus.COMPLETED.getCode()) {
            throw conflict("当前订单状态不可发起纠纷");
        }
        requireWithinAfterSaleWindow(order, orderStatus);
        if (disputes.existsByOrderId(order.getId())) {
            throw conflict("该订单已存在纠纷");
        }

        Dispute dispute = new Dispute();
        dispute.setOrderId(order.getId());
        dispute.setApplicantId(currentUserId);
        // 被申请人就是交易的另一方。
        dispute.setRespondentId(isBuyer ? order.getSellerId() : order.getBuyerId());
        dispute.setReasonType(request.reasonType());
        dispute.setStatement(request.statement().trim());
        dispute.setEvidenceJson(writeEvidence(request.evidence()));
        dispute.setStatus(DISPUTE_PENDING);
        dispute.setEvidenceVersion(1);

        try {
            disputes.insert(dispute);
        } catch (DuplicateKeyException exception) {
            // 并发下两个请求同时越过上面的 existsByOrderId 检查时，由唯一约束拦住第二个。
            throw conflict("该订单已存在纠纷");
        }

        // 条件更新：只有订单仍处于发起纠纷时读到的那个状态，才允许冻结，并顺带记录原状态。
        if (orders.enterDispute(order.getId(), currentUserId, orderStatus) == 0) {
            throw conflict("订单状态已变化，请刷新后重试");
        }

        insertOrderLog(
                order.getId(),
                orderStatus,
                OrderStatus.IN_DISPUTE.getCode(),
                isBuyer ? OPERATOR_BUYER : OPERATOR_SELLER,
                currentUserId,
                dispute.getStatement()
        );
        return new DisputeCreatedVO(dispute.getId());
    }

    /**
     * 已完成的订单只在完成后 7 天内可以发起纠纷，避免很早以前的订单被翻出来申诉。
     *
     * <p>已确认(1) 但尚未完成的订单不受此限，因为线下交易本身还在进行中。</p>
     */
    private void requireWithinAfterSaleWindow(TradeOrder order, int orderStatus) {
        if (orderStatus != OrderStatus.COMPLETED.getCode()) {
            return;
        }
        if (!DisputeRules.withinAfterSaleWindow(order.getFinishedAt())) {
            throw conflict("订单完成已超过 " + DisputeRules.AFTER_SALE_WINDOW_DAYS + " 天，不能再发起纠纷");
        }
    }

    /**
     * 管理员裁决纠纷，四种动作对应四种订单去向。
     *
     * <pre>
     * NEED_MORE      纠纷 → 1 待补材料，订单保持冻结（唯一不改订单状态的动作）
     * REJECT         纠纷 → 2 已驳回，  订单还原为 status_before_dispute
     * KEEP_COMPLETED 纠纷 → 3 维持完成，订单 → 2 已完成
     * CANCEL_TRADE   纠纷 → 4 取消交易，订单 → 3 已取消，可选择回补库存
     * </pre>
     */
    @Transactional
    public void handle(Long disputeId, HandleDisputeRequest request) {
        Long adminId = UserContext.requireCurrentUser().userId();

        Dispute dispute = disputes.selectById(disputeId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "纠纷不存在"));
        TradeOrder order = orders.selectById(dispute.getOrderId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        String note = trimToNull(request.note());

        switch (request.action()) {
            // 要求当事人补充材料：只推进纠纷状态，订单继续冻结，因此这里直接返回。
            case "NEED_MORE" -> {
                updateDisputeStatus(dispute, DISPUTE_NEED_MORE, adminId, note, request.evidenceVersion());
                return;
            }

            // 驳回纠纷：订单回到进入纠纷之前的状态。
            case "REJECT" -> {
                updateDisputeStatus(dispute, DISPUTE_REJECTED, adminId, note, request.evidenceVersion());
                int statusBeforeDispute = order.getStatusBeforeDispute();
                if (orders.restoreFromDispute(order.getId(), statusBeforeDispute) == 0) {
                    throw conflict("订单状态已变化");
                }
                insertOrderLog(
                        order.getId(),
                        OrderStatus.IN_DISPUTE.getCode(),
                        statusBeforeDispute,
                        OPERATOR_ADMIN,
                        adminId,
                        note
                );
            }

            // 维持交易完成：订单判定为已完成。
            case "KEEP_COMPLETED" -> {
                updateDisputeStatus(dispute, DISPUTE_KEEP_COMPLETED, adminId, note, request.evidenceVersion());
                if (orders.completeFromDispute(order.getId()) == 0) {
                    throw conflict("订单状态已变化");
                }
                // 只有“冻结前还没完成”的订单才需要累加成交数；
                // 若冻结前已是已完成，成交数在买家确认完成时就加过了，这里再加会重复计数。
                if (order.getStatusBeforeDispute() == OrderStatus.CONFIRMED.getCode()) {
                    users.incrementDealCount(order.getSellerId());
                }
                // 卖家成交数变了，商品详情里展示的信用摘要也要跟着失效。
                cacheInvalidator.invalidateAfterCommit(order.getSellerId());
                insertOrderLog(
                        order.getId(),
                        OrderStatus.IN_DISPUTE.getCode(),
                        OrderStatus.COMPLETED.getCode(),
                        OPERATOR_ADMIN,
                        adminId,
                        note
                );
            }

            // 取消交易：订单判定为已取消，是否退货回补库存由管理员勾选。
            case "CANCEL_TRADE" -> {
                updateDisputeStatus(dispute, DISPUTE_CANCEL_TRADE, adminId, note, request.evidenceVersion());
                if (orders.cancelFromDispute(order.getId()) == 0) {
                    throw conflict("订单状态已变化");
                }
                if (Boolean.TRUE.equals(request.restock())) {
                    products.restoreStockForCancelledOrder(order.getProductId(), order.getQuantity());
                }
                cacheInvalidator.invalidateAfterCommit(order.getSellerId());
                insertOrderLog(
                        order.getId(),
                        OrderStatus.IN_DISPUTE.getCode(),
                        OrderStatus.CANCELLED.getCode(),
                        OPERATOR_ADMIN,
                        adminId,
                        note
                );
            }

            default -> throw new BizException(ErrorCode.BAD_REQUEST, "未知处理动作");
        }
    }

    /**
     * 管理端纠纷游标分页列表，附带订单号、商品标题和买卖双方昵称。
     *
     * <p>一次多取一条记录：前 {@code pageSize} 条用于展示，多出的第 {@code pageSize + 1} 条
     * 只用于判断是否存在下一页。这样无需额外执行 COUNT(*)，也不会因为 OFFSET 很大而扫描大量
     * 已跳过的旧记录。</p>
     */
    public CursorPageResult<AdminDisputeVO> list(
            Integer status,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int pageSize
    ) {
        List<AdminDisputeRow> rows = disputes.selectAdminCursorPage(
                status,
                cursorCreatedAt,
                cursorId,
                pageSize + 1
        );
        boolean hasNext = rows.size() > pageSize;
        List<AdminDisputeRow> currentRows = hasNext ? rows.subList(0, pageSize) : rows;
        List<AdminDisputeVO> list = currentRows.stream()
                .map(this::toAdminVO)
                .toList();

        if (!hasNext) {
            return new CursorPageResult<>(list, false, null, null);
        }
        // 下一页从当前页最后一条记录之后继续读取，而不是从多取出的探测记录开始。
        AdminDisputeRow lastRow = currentRows.get(currentRows.size() - 1);
        return new CursorPageResult<>(list, true, lastRow.getCreatedAt(), lastRow.getId());
    }

    /**
     * 将 JOIN SQL 的读模型转换为既有接口 VO。
     *
     * <p>订单、商品和用户字段已在 Mapper 中一次查出；此处只负责解析 evidence JSON，
     * 不再执行任何数据库查询。</p>
     */
    private AdminDisputeVO toAdminVO(AdminDisputeRow dispute) {
        return new AdminDisputeVO(
                dispute.getId(),
                dispute.getOrderId(),
                dispute.getReasonType(),
                dispute.getStatement(),
                readEvidence(dispute.getEvidenceJson()),
                dispute.getStatus(),
                dispute.getEvidenceVersion(),
                nullToEmpty(dispute.getOrderNo()),
                nullToEmpty(dispute.getProductTitle()),
                nullToEmpty(dispute.getBuyerName()),
                nullToEmpty(dispute.getSellerName()),
                dispute.getOrderStatus(),
                dispute.getCreatedAt()
        );
    }

    /** 条件更新纠纷状态：只有仍处于待处理/待补材料的纠纷才能被裁决，避免两名管理员重复处理。 */
    private void updateDisputeStatus(Dispute dispute, int targetStatus, Long adminId, String note, Integer evidenceVersion) {
        if (disputes.updateHandled(dispute.getId(), targetStatus, adminId, note, evidenceVersion) == 0) {
            throw conflict("该纠纷已处理");
        }
    }

    /** 写一条订单状态流转日志，订单详情页的时间线就是读这张表。 */
    private void insertOrderLog(Long orderId, int fromStatus, int toStatus, int operatorType, Long operatorId, String reason) {
        TradeOrderLog log = new TradeOrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setReason(reason);
        orders.insertLog(log);
    }

    /** 证据图片地址列表以 JSON 字符串形式存进 dispute.evidence 列。 */
    private String writeEvidence(List<String> evidence) {
        try {
            return json.writeValueAsString(evidence == null ? List.of() : evidence);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** 读取证据列；历史数据异常时按“没有证据”处理，不能因为一条脏数据让整个列表接口报错。 */
    private List<String> readEvidence(String evidenceJson) {
        try {
            return evidenceJson == null ? List.of() : json.readValue(evidenceJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** JOIN 的关联字段可能因历史脏数据为 null，沿用改造前列表接口返回空字符串的契约。 */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    /** 只有申请人和被申请人可以查看纠纷详情，管理员走独立的管理端接口。 */
    public ParticipantDisputeDetailVO getForParticipant(Long disputeId) {
        Long userId = UserContext.requireCurrentUser().userId();
        Dispute dispute = findDispute(disputeId);
        if (participantRole(dispute, userId) < 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有纠纷当事人可以查看");
        }
        return buildParticipantDetail(dispute);
    }

    /** 订单详情页按订单查询纠纷，仍然复用当事人权限校验。 */
    public ParticipantDisputeDetailVO getForOrder(Long orderId) {
        Long userId = UserContext.requireCurrentUser().userId();
        Dispute dispute = disputes.selectByOrderId(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "纠纷不存在"));
        if (participantRole(dispute, userId) < 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有纠纷当事人可以查看");
        }
        return buildParticipantDetail(dispute);
    }

    /**
     * 补充材料采用“读旧版本 + 条件更新 + 写流水”的顺序。
     * 条件更新影响行数为 0 时，说明管理员或另一方已经更新了这份纠纷，不能继续写流水。
     */
    @Transactional
    public void appendEvidence(Long disputeId, AppendDisputeEvidenceRequest request) {
        Long userId = UserContext.requireCurrentUser().userId();
        Dispute dispute = findDispute(disputeId);
        int role = participantRole(dispute, userId);
        if (role < 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有纠纷当事人可以补充材料");
        }
        if (!Objects.equals(dispute.getStatus(), DISPUTE_NEED_MORE)) {
            throw conflict("当前纠纷不是待补充材料状态");
        }
        List<String> additions = request.evidence() == null ? List.of() : request.evidence();
        if (additions.size() > MAX_EVIDENCE_PER_APPEND) {
            throw new BizException(ErrorCode.BAD_REQUEST, "单次追加证据图片最多 5 张");
        }
        String statement = request.statement() == null ? null : request.statement().trim();
        if ((statement == null || statement.isBlank()) && additions.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "补充说明和证据不能同时为空");
        }
        List<String> merged = new ArrayList<>(readEvidence(dispute.getEvidenceJson()));
        merged.addAll(additions);
        if (merged.size() > MAX_EVIDENCE_TOTAL) {
            throw new BizException(ErrorCode.BAD_REQUEST, "累计证据图片最多保留 15 张");
        }
        int oldVersion = dispute.getEvidenceVersion() == null ? 1 : dispute.getEvidenceVersion();
        if (disputes.appendEvidence(disputeId, oldVersion, writeEvidence(merged)) == 0) {
            throw conflict("纠纷材料已被更新，请刷新后重试");
        }
        DisputeEvidenceLog log = new DisputeEvidenceLog();
        log.setDisputeId(disputeId);
        log.setOperatorId(userId);
        log.setOperatorRole(role);
        log.setEvidenceVersion(oldVersion + 1);
        log.setStatement(statement);
        log.setEvidenceJson(writeEvidence(additions));
        evidenceLogs.insert(log);
    }

    /**
     * 当事人只能看到处理纠纷所需的信息；信用摘要、评价、订单日志和 handler_id 属于管理端风控数据。
     */
    private ParticipantDisputeDetailVO buildParticipantDetail(Dispute dispute) {
        TradeOrder order = orders.selectById(dispute.getOrderId()).orElse(null);
        User applicant = users.selectById(dispute.getApplicantId()).orElse(null);
        User respondent = users.selectById(dispute.getRespondentId()).orElse(null);
        ProductSnapshot snapshot = order == null ? null : orders.selectSnapshotByOrderId(order.getId()).orElse(null);
        List<ParticipantDisputeDetailVO.EvidenceLogVO> logs = evidenceLogs.selectByDisputeId(dispute.getId()).stream()
                .map(log -> new ParticipantDisputeDetailVO.EvidenceLogVO(log.getId(), log.getOperatorId(),
                        log.getOperatorRole(), log.getEvidenceVersion(), log.getStatement(),
                        readEvidence(log.getEvidenceJson()), log.getCreatedAt()))
                .toList();
        ParticipantDisputeDetailVO.OrderSummary orderSummary = order == null ? null
                : new ParticipantDisputeDetailVO.OrderSummary(order.getId(), order.getOrderNo(),
                order.getQuantity(), order.getUnitPrice(), order.getTotalAmount(), order.getStatus(),
                order.getTradeTime(), order.getFinishedAt());
        return new ParticipantDisputeDetailVO(dispute.getId(), dispute.getOrderId(), dispute.getApplicantId(),
                dispute.getRespondentId(), dispute.getReasonType(), dispute.getStatement(),
                readEvidence(dispute.getEvidenceJson()), dispute.getStatus(), dispute.getEvidenceVersion(),
                dispute.getHandleNote(), dispute.getHandledAt(), dispute.getCreatedAt(), orderSummary, snapshot,
                participant(applicant), participant(respondent), logs);
    }

    /** 管理员详情固定聚合一条纠纷的关联数据，不按列表逐行查询。 */
    public DisputeDetailVO adminDetail(Long disputeId) {
        return buildDetail(findDispute(disputeId));
    }

    private DisputeDetailVO buildDetail(Dispute dispute) {
        TradeOrder order = orders.selectById(dispute.getOrderId()).orElse(null);
        User applicant = users.selectById(dispute.getApplicantId()).orElse(null);
        User respondent = users.selectById(dispute.getRespondentId()).orElse(null);
        ProductSnapshot snapshot = order == null ? null : orders.selectSnapshotByOrderId(order.getId()).orElse(null);
        TradeReview review = order == null ? null : reviews.selectByOrderId(order.getId());
        CreditSummary applicantCredit = users.selectCreditSummary(dispute.getApplicantId()).orElse(null);
        CreditSummary respondentCredit = users.selectCreditSummary(dispute.getRespondentId()).orElse(null);
        List<TradeOrderLog> logs = order == null ? List.of() : orders.selectLogsByOrderId(order.getId());
        List<DisputeDetailVO.EvidenceLogVO> evidenceLogs = this.evidenceLogs.selectByDisputeId(dispute.getId()).stream()
                .map(log -> new DisputeDetailVO.EvidenceLogVO(log.getId(), log.getOperatorId(), log.getOperatorRole(),
                        log.getEvidenceVersion(), log.getStatement(), readEvidence(log.getEvidenceJson()), log.getCreatedAt()))
                .toList();
        DisputeDetailVO.OrderSummary orderSummary = order == null ? null : new DisputeDetailVO.OrderSummary(
                order.getId(), order.getOrderNo(), order.getBuyerId(), order.getSellerId(), order.getProductId(),
                order.getQuantity(), order.getUnitPrice(), order.getTotalAmount(), order.getStatus(),
                order.getStatusBeforeDispute(), order.getRemark(), order.getTradeTime(), order.getFinishedAt());
        return new DisputeDetailVO(dispute.getId(), dispute.getOrderId(), dispute.getApplicantId(), dispute.getRespondentId(),
                dispute.getReasonType(), dispute.getStatement(), readEvidence(dispute.getEvidenceJson()), dispute.getStatus(),
                dispute.getEvidenceVersion(), dispute.getHandlerId(), dispute.getHandleNote(), dispute.getHandledAt(),
                dispute.getCreatedAt(), orderSummary, snapshot, participantAdmin(applicant), participantAdmin(respondent),
                applicantCredit, respondentCredit, review, logs, evidenceLogs);
    }

    private ParticipantDisputeDetailVO.Participant participant(User user) {
        return user == null ? null : new ParticipantDisputeDetailVO.Participant(user.getId(), user.getNickname(), user.getAvatar(), user.getCampus());
    }

    private DisputeDetailVO.Participant participantAdmin(User user) {
        return user == null ? null : new DisputeDetailVO.Participant(user.getId(), user.getNickname(), user.getAvatar(), user.getCampus());
    }

    private int participantRole(Dispute dispute, Long userId) {
        if (Objects.equals(dispute.getApplicantId(), userId)) return 0;
        if (Objects.equals(dispute.getRespondentId(), userId)) return 1;
        return -1;
    }

    private Dispute findDispute(Long disputeId) {
        return disputes.selectById(disputeId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "纠纷不存在"));
    }
}
