package com.campus.trade.dispute.mapper;

import com.campus.trade.dispute.entity.DisputeEvidenceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DisputeEvidenceLogMapper {
    int insert(DisputeEvidenceLog log);
    List<DisputeEvidenceLog> selectByDisputeId(@Param("disputeId") Long disputeId);
}
