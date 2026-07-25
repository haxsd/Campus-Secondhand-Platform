package com.campus.trade.dispute.mapper;
import com.campus.trade.dispute.entity.Dispute; import org.apache.ibatis.annotations.*; import java.util.*;
/** 纠纷持久层；订单状态流转仍由 OrderMapper 完成。 */
@Mapper public interface DisputeMapper { boolean existsByOrderId(@Param("orderId") Long id); Optional<Dispute> selectById(@Param("id") Long id); int insert(Dispute dispute); List<Dispute> selectPage(@Param("status") Integer status,@Param("pageSize") int pageSize,@Param("offset") int offset); long count(@Param("status") Integer status); int updateHandled(@Param("id") Long id,@Param("targetStatus") Integer targetStatus,@Param("handlerId") Long handlerId,@Param("note") String note); }
