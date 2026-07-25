package com.campus.trade.dispute.vo;
import com.fasterxml.jackson.annotation.JsonFormat; import java.time.LocalDateTime; import java.util.List;
/** 管理端纠纷列表项，包含订单、商品和双方昵称，避免管理员逐项额外查询。 */
public record AdminDisputeVO(Long id,Long orderId,Integer reasonType,String statement,List<String> evidence,Integer status,String orderNo,String productTitle,String buyerName,String sellerName,Integer orderStatus,@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt){}
