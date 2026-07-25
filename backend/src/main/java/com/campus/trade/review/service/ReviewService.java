package com.campus.trade.review.service;
import com.campus.trade.common.context.UserContext; import com.campus.trade.common.exception.BizException; import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.order.entity.TradeOrder; import com.campus.trade.order.mapper.OrderMapper; import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.review.dto.CreateReviewRequest; import com.campus.trade.review.entity.TradeReview; import com.campus.trade.review.mapper.ReviewMapper;
import org.springframework.dao.DuplicateKeyException; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
/** 评价规则：仅买家可评价已完成订单，且一单一评。 */
@Service public class ReviewService {
 private final ReviewMapper reviewMapper; private final OrderMapper orderMapper;
 public ReviewService(ReviewMapper reviewMapper, OrderMapper orderMapper) { this.reviewMapper=reviewMapper; this.orderMapper=orderMapper; }
 @Transactional public void create(CreateReviewRequest request) {
  Long userId=UserContext.requireCurrentUser().userId(); TradeOrder order=orderMapper.selectById(request.orderId()).orElseThrow(()->new BizException(ErrorCode.NOT_FOUND,"订单不存在"));
  if(!Objects.equals(order.getBuyerId(),userId)) throw new BizException(ErrorCode.FORBIDDEN,"只有买家可以评价");
  if(!Objects.equals(order.getStatus(),OrderStatus.COMPLETED.getCode())) throw new BizException(ErrorCode.CONFLICT,"仅已完成的订单可以评价");
  if(reviewMapper.existsByOrderId(order.getId())) throw new BizException(ErrorCode.CONFLICT,"该订单已评价");
  TradeReview review=new TradeReview(); review.setOrderId(order.getId()); review.setReviewerId(userId); review.setSellerId(order.getSellerId()); review.setRating(request.rating()); review.setContent(trimToNull(request.content())); review.setTags(trimToNull(request.tags())); review.setVisible(1);
  try { reviewMapper.insert(review); } catch(DuplicateKeyException e) { throw new BizException(ErrorCode.CONFLICT,"该订单已评价"); }
  reviewMapper.recalculateSellerCredit(order.getSellerId());
 }
 private String trimToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
}
