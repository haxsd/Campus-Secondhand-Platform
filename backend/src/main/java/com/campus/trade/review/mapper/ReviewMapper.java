package com.campus.trade.review.mapper;
import com.campus.trade.review.entity.TradeReview; import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
/** 评价写入及卖家信用摘要重算 SQL。 */
@Mapper public interface ReviewMapper {
    boolean existsByOrderId(@Param("orderId") Long orderId);
    int insert(TradeReview review);
    /** 只统计 visible=1 的评价，管理员隐藏评价后可复用本 SQL 重算。 */
    int recalculateSellerCredit(@Param("sellerId") Long sellerId);
}
