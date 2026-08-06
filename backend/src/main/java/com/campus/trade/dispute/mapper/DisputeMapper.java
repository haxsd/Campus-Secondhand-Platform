package com.campus.trade.dispute.mapper;

import com.campus.trade.dispute.entity.Dispute;
import com.campus.trade.dispute.model.AdminDisputeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface DisputeMapper {
    boolean existsByOrderId(@Param("orderId") Long orderId);
    Optional<Dispute> selectById(@Param("id") Long id);
    Optional<Dispute> selectByOrderId(@Param("orderId") Long orderId);
    int insert(Dispute dispute);
    List<AdminDisputeRow> selectAdminCursorPage(@Param("status") Integer status, @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt, @Param("cursorId") Long cursorId, @Param("limit") int limit);
    int updateHandled(@Param("id") Long id, @Param("targetStatus") Integer targetStatus, @Param("handlerId") Long handlerId, @Param("note") String note, @Param("evidenceVersion") Integer evidenceVersion);
    int appendEvidence(@Param("id") Long id, @Param("evidenceVersion") Integer evidenceVersion, @Param("evidenceJson") String evidenceJson);
}
