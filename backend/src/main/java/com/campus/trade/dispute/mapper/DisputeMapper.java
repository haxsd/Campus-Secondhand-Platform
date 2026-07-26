package com.campus.trade.dispute.mapper;

import com.campus.trade.dispute.entity.Dispute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 纠纷数据访问接口，SQL 写在 resources/mapper/dispute/DisputeMapper.xml。
 *
 * <p>纠纷本身只负责自己这张表；订单状态的变更统一由 OrderMapper 的条件更新完成，
 * 避免同一张表被两个 Mapper 分别修改。</p>
 */
@Mapper
public interface DisputeMapper {

    /** 判断该订单是否已经存在纠纷，用于给用户友好的提示（最终仍由唯一约束兜底）。 */
    boolean existsByOrderId(@Param("orderId") Long orderId);

    Optional<Dispute> selectById(@Param("id") Long id);

    int insert(Dispute dispute);

    /** 管理端分页查询，status 为 null 时表示不限状态。 */
    List<Dispute> selectPage(
            @Param("status") Integer status,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset
    );

    long count(@Param("status") Integer status);

    /**
     * 条件更新纠纷处理结果。
     *
     * <p>SQL 带有 {@code WHERE status IN (0,1)}，因此只有仍待处理或待补材料的纠纷才能被裁决，
     * 两个管理员同时提交时只有一个能拿到影响行数 1。</p>
     *
     * @return 影响行数，0 表示该纠纷已被别人处理
     */
    int updateHandled(
            @Param("id") Long id,
            @Param("targetStatus") Integer targetStatus,
            @Param("handlerId") Long handlerId,
            @Param("note") String note
    );
}
