package com.campus.trade.review.mapper;

import com.campus.trade.review.entity.TradeReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewMapper {
    boolean existsByOrderId(@Param("orderId") Long orderId);
    int insert(TradeReview review);
    int recalculateSellerCredit(@Param("sellerId") Long sellerId);
    TradeReview selectByOrderId(@Param("orderId") Long orderId);
}
