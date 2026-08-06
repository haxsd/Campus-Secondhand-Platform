package com.campus.trade.dispute.mapper;

import com.campus.trade.dispute.entity.DisputeEvidenceLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 纠纷证据追加流水的数据访问接口。 */
@Mapper
public interface DisputeEvidenceLogMapper {
    int insert(DisputeEvidenceLog log);

    List<DisputeEvidenceLog> selectByDisputeId(@Param("disputeId") Long disputeId);
}
