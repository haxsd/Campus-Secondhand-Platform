package com.campus.trade.ai.dispute;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 纠纷 Agent 运行记录的数据访问接口。
 *
 * <p>状态更新均带有前置状态条件，避免异步线程重复回写同一条运行记录。</p>
 */
@Mapper
public interface DisputeAgentRunMapper {
    /** 创建待执行记录。 */
    int insert(DisputeAgentRunEntity run);

    /** 按 runId 查询运行记录。 */
    DisputeAgentRunEntity selectByRunId(@Param("runId") String runId);

    /** 查询当前证据版本已成功的结果，用于服务端幂等复用。 */
    DisputeAgentRunEntity selectSucceeded(
            @Param("disputeId") Long disputeId,
            @Param("evidenceVersion") Integer evidenceVersion
    );

    /** 查询最新运行记录，供管理员轮询。 */
    DisputeAgentRunEntity selectLatest(@Param("disputeId") Long disputeId);

    /** 只有 PENDING 可以转 RUNNING，保证任务只被一个执行线程领取。 */
    int markRunning(@Param("runId") String runId);

    /** 仅允许 RUNNING 任务写入成功结果。 */
    int markSuccess(
            @Param("runId") String runId,
            @Param("resultJson") String resultJson
    );

    /** 失败、输出非法和超时都通过统一状态写回。 */
    int markFailure(
            @Param("runId") String runId,
            @Param("status") String status,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    /** 当前证据或纠纷状态变化时，使运行结果失效。 */
    int markStale(@Param("runId") String runId, @Param("errorCode") String errorCode);

    /** 管理员采纳只写审计字段，不执行真实裁决。 */
    int markAdopted(
            @Param("runId") String runId,
            @Param("adminId") Long adminId,
            @Param("action") String action
    );

    /** 查询超过允许执行时长的任务，供超时扫描器处理。 */
    List<DisputeAgentRunEntity> selectStale(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit
    );
}
