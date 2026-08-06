package com.campus.trade.ai.dispute;

/** 纠纷辅助分析的一次运行状态。 */
public enum DisputeAgentStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED, INVALID_OUTPUT, STALE, TIMEOUT
}
