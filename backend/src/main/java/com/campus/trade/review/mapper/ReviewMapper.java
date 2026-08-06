package com.campus.trade.review.mapper;

import com.campus.trade.review.entity.TradeReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 评价数据访问接口，SQL 写在 resources/mapper/review/ReviewMapper.xml。 */
@Mapper
public interface ReviewMapper {

    /** 该订单是否已评价，用于提前给出友好提示（最终由 order_id 唯一约束兜底）。 */
    boolean existsByOrderId(@Param("orderId") Long orderId);

    int insert(TradeReview review);

    /**
     * 依据该卖家全部可见评价，整体重算信用摘要（评价数、平均分、好评率、差评数、信用分）。
     *
     * <p>刻意使用「整体重算」而不是「在原值上累加」：只要 trade_review 是对的，
     * 摘要表就一定能被重新算对，不会因为某次更新失败而永久跑偏。</p>
     */
    int recalculateSellerCredit(@Param("sellerId") Long sellerId);

    /** 按订单读取评价，纠纷详情展示下单后的评价事实。 */
    TradeReview selectByOrderId(@Param("orderId") Long orderId);
}
