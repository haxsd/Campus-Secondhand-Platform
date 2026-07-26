package com.campus.trade.order.mapper;

import com.campus.trade.order.entity.ProductSnapshot;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 订单域数据库访问接口。
 *
 * <p>事务、身份和状态机判断都在 OrderService；Mapper 仅执行明确的数据库读写和条件更新。</p>
 */
@Mapper
public interface OrderMapper {

    /** 根据买家和客户端幂等键查询已创建订单。 */
    Optional<TradeOrder> selectByBuyerIdAndRequestId(
            @Param("buyerId") Long buyerId,
            @Param("requestId") String requestId
    );

    /** 按主键读取订单主记录；是否可访问由 Service 根据买卖双方 ID 判断。 */
    Optional<TradeOrder> selectById(@Param("id") Long id);

    /** 查询当前用户作为买家或卖家的订单页。 */
    List<TradeOrder> selectPageByParticipant(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("status") Integer status,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset
    );

    /** 统计与 selectPageByParticipant 使用相同条件的订单数量。 */
    long countByParticipant(
            @Param("userId") Long userId,
            @Param("role") String role,
            @Param("status") Integer status
    );

    /** 插入订单并把自增主键回填到 order.id。 */
    int insert(TradeOrder order);

    /** 插入一对一的商品快照。 */
    int insertSnapshot(ProductSnapshot snapshot);

    /** 查询订单商品快照。 */
    Optional<ProductSnapshot> selectSnapshotByOrderId(@Param("orderId") Long orderId);

    /** 按时间顺序查询状态日志。 */
    List<TradeOrderLog> selectLogsByOrderId(@Param("orderId") Long orderId);

    /** 写入一条可追溯的状态流转日志。 */
    int insertLog(TradeOrderLog log);

    /** 卖家确认：仅待确认订单可以从 0 流转为 1。 */
    int confirmBySeller(@Param("id") Long id, @Param("sellerId") Long sellerId);

    /** 买卖双方都可取消待确认或已确认订单。 */
    int cancelByParticipant(@Param("id") Long id, @Param("userId") Long userId);

    /** 买家确认完成：仅已确认订单可从 1 流转为 2。 */
    int completeByBuyer(@Param("id") Long id, @Param("buyerId") Long buyerId);

    int enterDispute(@Param("id") Long id, @Param("userId") Long userId, @Param("fromStatus") Integer fromStatus);
    int restoreFromDispute(@Param("id") Long id, @Param("targetStatus") Integer targetStatus);
    int completeFromDispute(@Param("id") Long id);
    int cancelFromDispute(@Param("id") Long id);

    /** 分批扫描已超过卖家确认期限的待确认订单 ID。 */
    List<Long> selectExpiredPendingOrderIds(@Param("limit") int limit);

    /** 系统超时取消的条件更新；即使扫描结果已过时也不会覆盖卖家刚确认的订单。 */
    int timeoutCancelBySystem(@Param("id") Long id);
}
