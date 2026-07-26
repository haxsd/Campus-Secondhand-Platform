package com.campus.trade.dispute.service;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.PageResult;
import com.campus.trade.dispute.dto.CreateDisputeRequest;
import com.campus.trade.dispute.dto.HandleDisputeRequest;
import com.campus.trade.dispute.entity.Dispute;
import com.campus.trade.dispute.mapper.DisputeMapper;
import com.campus.trade.dispute.model.DisputeRules;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.SellerDetailCacheInvalidator;
import com.campus.trade.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final DisputeMapper disputes;
    private final OrderMapper orders;
    private final ProductMapper products;
    private final UserMapper users;
    private final SellerDetailCacheInvalidator cacheInvalidator;
    private final ObjectMapper json;

    public DisputeService(
            DisputeMapper disputes,
            OrderMapper orders,
            ProductMapper products,
            UserMapper users,
            SellerDetailCacheInvalidator cacheInvalidator,
            ObjectMapper json
    ) {
        this.disputes = disputes;
        this.orders = orders;
        this.products = products;
        this.users = users;
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
                updateDisputeStatus(dispute, DISPUTE_NEED_MORE, adminId, note);
                return;
            }

            // 驳回纠纷：订单回到进入纠纷之前的状态。
            case "REJECT" -> {
                updateDisputeStatus(dispute, DISPUTE_REJECTED, adminId, note);
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
                updateDisputeStatus(dispute, DISPUTE_KEEP_COMPLETED, adminId, note);
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
                updateDisputeStatus(dispute, DISPUTE_CANCEL_TRADE, adminId, note);
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

    /** 管理端纠纷分页列表，附带订单号、商品标题和买卖双方昵称，便于管理员直接判断。 */
    public PageResult<AdminDisputeVO> list(Integer status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AdminDisputeVO> list = disputes.selectPage(status, pageSize, offset)
                .stream()
                .map(this::toAdminVO)
                .toList();
        return new PageResult<>(list, disputes.count(status), page, pageSize);
    }

    /**
     * 把纠纷记录补齐成管理端需要的展示对象。
     *
     * <p>注意：这里对每条纠纷都会再查订单、商品和买卖双方，属于典型的 N+1 查询。
     * 当前管理端纠纷量很小，可以接受；数据量上来后应改为在 XML 里一次 JOIN 查出。</p>
     */
    private AdminDisputeVO toAdminVO(Dispute dispute) {
        TradeOrder order = orders.selectById(dispute.getOrderId()).orElse(null);
        return new AdminDisputeVO(
                dispute.getId(),
                dispute.getOrderId(),
                dispute.getReasonType(),
                dispute.getStatement(),
                readEvidence(dispute.getEvidenceJson()),
                dispute.getStatus(),
                order == null ? "" : order.getOrderNo(),
                order == null ? "" : products.selectById(order.getProductId())
                        .map(product -> product.getTitle()).orElse(""),
                order == null ? "" : users.selectById(order.getBuyerId())
                        .map(user -> user.getNickname()).orElse(""),
                order == null ? "" : users.selectById(order.getSellerId())
                        .map(user -> user.getNickname()).orElse(""),
                order == null ? null : order.getStatus(),
                dispute.getCreatedAt()
        );
    }

    /** 条件更新纠纷状态：只有仍处于待处理/待补材料的纠纷才能被裁决，避免两名管理员重复处理。 */
    private void updateDisputeStatus(Dispute dispute, int targetStatus, Long adminId, String note) {
        if (disputes.updateHandled(dispute.getId(), targetStatus, adminId, note) == 0) {
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

    private BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }
}
