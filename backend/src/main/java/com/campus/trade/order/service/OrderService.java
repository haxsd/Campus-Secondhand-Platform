package com.campus.trade.order.service;

import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.PageResult;
import com.campus.trade.order.dto.CreateOrderRequest;
import com.campus.trade.order.entity.ProductSnapshot;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.order.vo.OrderCreatedVO;
import com.campus.trade.order.vo.OrderDetailVO;
import com.campus.trade.order.vo.OrderListItemVO;
import com.campus.trade.order.vo.OrderLogVO;
import com.campus.trade.order.vo.OrderPartyVO;
import com.campus.trade.order.vo.OrderSnapshotVO;
import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.ProductDetailCacheService;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.mapper.UserMapper;
import com.campus.trade.review.mapper.ReviewMapper;
import com.campus.trade.dispute.mapper.DisputeMapper;
import com.campus.trade.dispute.model.DisputeRules;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 订单业务服务。
 *
 * <p>这里是订单域的规则中心：下单幂等、库存占用、买卖双方权限、状态机以及状态日志。
 * Mapper 中的条件 UPDATE 负责并发下最后一次校验，Service 的预检负责返回清晰业务提示。</p>
 */
@Service
public class OrderService {

    /** 订单号以创建时间开头，后缀使用 UUID 片段降低多线程、多实例碰撞概率。 */
    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 订单确认期限：卖家需在 24 小时内确认；自动超时取消由后续定时任务阶段实现。 */
    private static final long CONFIRM_TIMEOUT_HOURS = 24L;

    /** 订单状态日志的操作方编码，与 trade_order_log.operator_type 表注释一致。 */
    private static final int OPERATOR_BUYER = 0;
    private static final int OPERATOR_SELLER = 1;

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ProductDetailCacheService productDetailCacheService;
    private final ObjectMapper objectMapper;
    private final ReviewMapper reviewMapper;
    private final DisputeMapper disputeMapper;

    public OrderService(
            OrderMapper orderMapper,
            ProductMapper productMapper,
            UserMapper userMapper,
            ProductDetailCacheService productDetailCacheService,
            ObjectMapper objectMapper,
            ReviewMapper reviewMapper,
            DisputeMapper disputeMapper
    ) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.productDetailCacheService = productDetailCacheService;
        this.objectMapper = objectMapper;
        this.reviewMapper = reviewMapper;
        this.disputeMapper = disputeMapper;
    }

    /**
     * 创建订单并扣减库存。
     *
     * <p>顺序刻意设计为：先检查幂等记录，再写入订单和快照，最后原子扣库存。
     * 同一 requestId 并发到达时，数据库唯一索引阻止第二条订单写入；库存扣减失败时整个事务回滚，
     * 因而不会产生“订单存在但未占库存”或“重复请求重复扣库存”的中间状态。</p>
     */
    @Transactional
    public OrderCreatedVO create(CreateOrderRequest request) {
        Long buyerId = UserContext.requireCurrentUser().userId();

        // 普通重试直接返回首次创建结果，不再读取商品或重复扣库存。
        TradeOrder existed = orderMapper.selectByBuyerIdAndRequestId(buyerId, request.requestId()).orElse(null);
        if (existed != null) {
            return toCreatedVO(existed);
        }

        Product product = productMapper.selectById(request.productId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (Objects.equals(product.getSellerId(), buyerId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不能购买自己发布的商品");
        }

        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setRequestId(request.requestId().trim());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setQuantity(request.quantity());
        // 金额只能由数据库中的商品价格计算，绝不使用浏览器传来的总价。
        order.setUnitPrice(product.getPrice());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setTradeTime(request.tradeTime());
        order.setTradePlace(request.tradePlace().trim());
        order.setRemark(trimToNull(request.remark()));
        order.setStatus(OrderStatus.PENDING_CONFIRM.getCode());
        order.setConfirmDeadline(LocalDateTime.now().plusHours(CONFIRM_TIMEOUT_HOURS));

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            // 两个同 requestId 的请求同时越过首次查询时，唯一索引仍会拦住第二个请求。
            // 此时第二个请求尚未扣库存，直接读取并返回第一个请求已创建的订单即可。
            return orderMapper.selectByBuyerIdAndRequestId(buyerId, request.requestId())
                    .map(this::toCreatedVO)
                    .orElseThrow(() -> exception);
        }

        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setOrderId(order.getId());
        snapshot.setProductId(product.getId());
        snapshot.setTitle(product.getTitle());
        snapshot.setDescription(product.getDescription());
        snapshot.setPrice(product.getPrice());
        snapshot.setItemCondition(product.getItemCondition());
        snapshot.setCampus(product.getCampus());
        snapshot.setTradePlace(product.getTradePlace());
        snapshot.setImagesJson(writeImages(productMapper.selectImageUrlsByProductId(product.getId())));
        orderMapper.insertSnapshot(snapshot);

        // 最终以 SQL 条件 UPDATE 判断商品仍在售且库存足够，避免并发超卖。
        if (productMapper.decreaseStockForOrder(product.getId(), request.quantity()) == 0) {
            throw conflict("库存不足或商品已变化，请刷新后重试");
        }
        invalidateProductDetailCacheAfterCommit(product.getId());
        return toCreatedVO(order);
    }

    /** 查询当前登录用户作为买家或卖家的订单页。 */
    public PageResult<OrderListItemVO> listMine(String role, Integer status, int page, int pageSize) {
        Long userId = UserContext.requireCurrentUser().userId();
        int offset = (page - 1) * pageSize;
        List<OrderListItemVO> list = orderMapper.selectPageByParticipant(userId, role, status, pageSize, offset)
                .stream()
                .map(this::toListItemVO)
                .toList();
        long total = orderMapper.countByParticipant(userId, role, status);
        return new PageResult<>(list, total, page, pageSize);
    }

    /** 查询订单详情，并按当前用户身份计算前端操作按钮的 can* 标记。 */
    public OrderDetailVO getDetail(Long orderId) {
        CurrentUser currentUser = UserContext.requireCurrentUser();
        TradeOrder order = requireAccessibleOrder(orderId, currentUser.userId());
        return toDetailVO(order, currentUser.userId());
    }

    /** 卖家确认：待确认(0) → 已确认(1)。 */
    @Transactional
    public void confirm(Long orderId) {
        Long userId = UserContext.requireCurrentUser().userId();
        TradeOrder order = requireAccessibleOrder(orderId, userId);
        if (!Objects.equals(order.getSellerId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有卖家可以确认订单");
        }
        if (!Objects.equals(order.getStatus(), OrderStatus.PENDING_CONFIRM.getCode())) {
            throw conflict("订单状态已变化，请刷新后重试");
        }
        if (orderMapper.confirmBySeller(orderId, userId) == 0) {
            throw conflict("订单状态已变化，请刷新后重试");
        }
        insertLog(orderId, OrderStatus.PENDING_CONFIRM.getCode(), OrderStatus.CONFIRMED.getCode(), OPERATOR_SELLER, userId, null);
    }

    /**
     * 买家或卖家取消：待确认/已确认 → 已取消，并把订单占用的库存归还给商品。
     */
    @Transactional
    public void cancel(Long orderId, String reason) {
        Long userId = UserContext.requireCurrentUser().userId();
        TradeOrder order = requireAccessibleOrder(orderId, userId);
        if (!isCancellable(order.getStatus())) {
            throw conflict("当前订单状态不能取消");
        }
        if (orderMapper.cancelByParticipant(orderId, userId) == 0) {
            throw conflict("订单状态已变化，请刷新后重试");
        }

        // 恢复库存与取消订单同处一个事务；任一步失败都会一起回滚。
        productMapper.restoreStockForCancelledOrder(order.getProductId(), order.getQuantity());
        int operatorType = Objects.equals(order.getBuyerId(), userId) ? OPERATOR_BUYER : OPERATOR_SELLER;
        insertLog(orderId, order.getStatus(), OrderStatus.CANCELLED.getCode(), operatorType, userId, trimToNull(reason));
        invalidateProductDetailCacheAfterCommit(order.getProductId());
    }

    /** 买家确认完成：已确认(1) → 已完成(2)。 */
    @Transactional
    public void complete(Long orderId) {
        Long userId = UserContext.requireCurrentUser().userId();
        TradeOrder order = requireAccessibleOrder(orderId, userId);
        if (!Objects.equals(order.getBuyerId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有买家可以确认完成订单");
        }
        if (!Objects.equals(order.getStatus(), OrderStatus.CONFIRMED.getCode())) {
            throw conflict("当前订单状态不能确认完成");
        }
        if (orderMapper.completeByBuyer(orderId, userId) == 0) {
            throw conflict("订单状态已变化，请刷新后重试");
        }
        insertLog(orderId, OrderStatus.CONFIRMED.getCode(), OrderStatus.COMPLETED.getCode(), OPERATOR_BUYER, userId, null);
        // 成交次数只在条件更新成功后累加，重复点击完成接口不会重复累计。
        userMapper.incrementDealCount(order.getSellerId());
        // 商品详情会展示卖家成交次数，清除该卖家商品的详情缓存以便立即反映最新信用摘要。
        invalidateSellerProductDetailCachesAfterCommit(order.getSellerId());
    }

    private TradeOrder requireAccessibleOrder(Long orderId, Long userId) {
        TradeOrder order = orderMapper.selectById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));
        if (!Objects.equals(order.getBuyerId(), userId) && !Objects.equals(order.getSellerId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看或操作该订单");
        }
        return order;
    }

    private OrderListItemVO toListItemVO(TradeOrder order) {
        return new OrderListItemVO(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                toPartyVO(order.getBuyerId()),
                toPartyVO(order.getSellerId()),
                toSnapshotVO(requireSnapshot(order.getId()))
        );
    }

    private OrderDetailVO toDetailVO(TradeOrder order, Long currentUserId) {
        boolean isBuyer = Objects.equals(order.getBuyerId(), currentUserId);
        boolean isSeller = Objects.equals(order.getSellerId(), currentUserId);
        int status = order.getStatus();
        return new OrderDetailVO(
                order.getId(),
                order.getOrderNo(),
                status,
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalAmount(),
                order.getTradeTime(),
                order.getTradePlace(),
                order.getRemark(),
                order.getConfirmDeadline(),
                order.getFinishedAt(),
                order.getCreatedAt(),
                toPartyVO(order.getBuyerId()),
                toPartyVO(order.getSellerId()),
                toSnapshotVO(requireSnapshot(order.getId())),
                orderMapper.selectLogsByOrderId(order.getId()).stream().map(this::toLogVO).toList(),
                isSeller && status == OrderStatus.PENDING_CONFIRM.getCode(),
                (isBuyer || isSeller) && isCancellable(status),
                isBuyer && status == OrderStatus.CONFIRMED.getCode(),
                isBuyer && status == OrderStatus.COMPLETED.getCode() && !reviewMapper.existsByOrderId(order.getId()),
                (isBuyer || isSeller) && canDispute(order, status)
        );
    }

    /**
     * 是否还能对该订单发起纠纷：状态允许、未发起过，且已完成订单仍在售后窗口内。
     *
     * <p>这里的判断必须与 DisputeService 完全一致，否则前端会出现“按钮能点但一点就 409”。
     * 因此窗口规则统一放在 {@link DisputeRules} 里，两边共用同一份代码。</p>
     */
    private boolean canDispute(TradeOrder order, int status) {
        if (status != OrderStatus.CONFIRMED.getCode() && status != OrderStatus.COMPLETED.getCode()) {
            return false;
        }
        if (status == OrderStatus.COMPLETED.getCode()
                && !DisputeRules.withinAfterSaleWindow(order.getFinishedAt())) {
            return false;
        }
        return !disputeMapper.existsByOrderId(order.getId());
    }

    private ProductSnapshot requireSnapshot(Long orderId) {
        return orderMapper.selectSnapshotByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("订单缺少商品快照，订单 ID=" + orderId));
    }

    private OrderPartyVO toPartyVO(Long userId) {
        User user = userMapper.selectById(userId)
                .orElseThrow(() -> new IllegalStateException("订单关联用户不存在，用户 ID=" + userId));
        return new OrderPartyVO(user.getId(), user.getNickname(), user.getAvatar());
    }

    private OrderSnapshotVO toSnapshotVO(ProductSnapshot snapshot) {
        return new OrderSnapshotVO(
                snapshot.getTitle(),
                snapshot.getDescription(),
                snapshot.getPrice(),
                snapshot.getItemCondition(),
                snapshot.getCampus(),
                snapshot.getTradePlace(),
                readImages(snapshot.getImagesJson())
        );
    }

    private OrderLogVO toLogVO(TradeOrderLog log) {
        return new OrderLogVO(log.getFromStatus(), log.getToStatus(), log.getOperatorType(), log.getReason(), log.getCreatedAt());
    }

    private OrderCreatedVO toCreatedVO(TradeOrder order) {
        return new OrderCreatedVO(order.getId(), order.getOrderNo(), order.getStatus(), order.getConfirmDeadline());
    }

    private void insertLog(Long orderId, int fromStatus, int toStatus, int operatorType, Long operatorId, String reason) {
        TradeOrderLog log = new TradeOrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setReason(reason);
        orderMapper.insertLog(log);
    }

    private boolean isCancellable(Integer status) {
        return Objects.equals(status, OrderStatus.PENDING_CONFIRM.getCode())
                || Objects.equals(status, OrderStatus.CONFIRMED.getCode());
    }

    private String generateOrderNo() {
        // “O + 17 位时间 + 12 位 UUID”共 30 位，小于 order_no 的 32 字符限制。
        return "O" + ORDER_NO_TIME_FORMAT.format(LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String writeImages(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("商品图片快照序列化失败", exception);
        }
    }

    private List<String> readImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("商品图片快照数据损坏", exception);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    /** 数据库提交成功后才删商品详情缓存，避免事务回滚造成无意义缓存失效。 */
    private void invalidateProductDetailCacheAfterCommit(Long productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productDetailCacheService.invalidate(productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productDetailCacheService.invalidate(productId);
            }
        });
    }

    /**
     * 卖家信用摘要变化后，失效该卖家所有商品详情缓存。
     *
     * <p>详情缓存中嵌入了 seller.dealCount；不做这一步会让成交次数最多延迟缓存 TTL 才更新。
     * 只删除键，不直接重建，下一次用户访问时自然回源 MySQL。</p>
     */
    private void invalidateSellerProductDetailCachesAfterCommit(Long sellerId) {
        Runnable invalidateAction = () -> productMapper.selectBySeller(sellerId, null)
                .forEach(product -> productDetailCacheService.invalidate(product.getId()));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidateAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidateAction.run();
            }
        });
    }
}
