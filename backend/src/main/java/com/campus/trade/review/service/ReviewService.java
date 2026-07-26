package com.campus.trade.review.service;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.product.service.SellerDetailCacheInvalidator;
import com.campus.trade.review.dto.CreateReviewRequest;
import com.campus.trade.review.entity.TradeReview;
import com.campus.trade.review.mapper.ReviewMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 评价业务：只有买家能对自己的已完成订单评价，且一单只能评一次。
 *
 * <p>评价写入后会同步重算卖家的信用摘要（user_credit_summary），
 * 并在事务提交后清除该卖家所有商品的详情缓存，让页面上的评分立即生效。</p>
 */
@Service
public class ReviewService {

    /** 评价默认可见；管理员可以在数据库层把 visible 置 0 隐藏违规评价。 */
    private static final int VISIBLE = 1;

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final SellerDetailCacheInvalidator cacheInvalidator;

    public ReviewService(
            ReviewMapper reviewMapper,
            OrderMapper orderMapper,
            SellerDetailCacheInvalidator cacheInvalidator
    ) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.cacheInvalidator = cacheInvalidator;
    }

    /**
     * 提交一条订单评价。
     *
     * <p>三道业务校验：只有买家能评、只有已完成订单能评、一单一评。
     * 其中“一单一评”除了先查一次以外，还依赖 trade_review 表上 order_id 的唯一约束兜底，
     * 因为并发下两个请求可能同时通过 existsByOrderId 检查。</p>
     */
    @Transactional
    public void create(CreateReviewRequest request) {
        Long currentUserId = UserContext.requireCurrentUser().userId();

        TradeOrder order = orderMapper.selectById(request.orderId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "订单不存在"));

        if (!Objects.equals(order.getBuyerId(), currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有买家可以评价");
        }
        if (!Objects.equals(order.getStatus(), OrderStatus.COMPLETED.getCode())) {
            throw new BizException(ErrorCode.CONFLICT, "仅已完成的订单可以评价");
        }
        if (reviewMapper.existsByOrderId(order.getId())) {
            throw new BizException(ErrorCode.CONFLICT, "该订单已评价");
        }

        TradeReview review = new TradeReview();
        review.setOrderId(order.getId());
        review.setReviewerId(currentUserId);
        // 被评价人固定取订单上的卖家，不接受前端传入。
        review.setSellerId(order.getSellerId());
        review.setRating(request.rating());
        review.setContent(trimToNull(request.content()));
        review.setTags(trimToNull(request.tags()));
        review.setVisible(VISIBLE);

        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.CONFLICT, "该订单已评价");
        }

        // 整体重算而不是增量累加：即使中途出过错，下一次重算也能自动纠正回来。
        reviewMapper.recalculateSellerCredit(order.getSellerId());
        // 商品详情里内嵌了卖家评分，评分变了要让缓存失效（延后到事务提交之后执行）。
        cacheInvalidator.invalidateAfterCommit(order.getSellerId());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
