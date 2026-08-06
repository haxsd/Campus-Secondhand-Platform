package com.campus.trade.ai.dispute;

/** 管理员轮询时看到的运行状态和结构化建议。 */
public record DisputeAgentRunView(
        String runId, String status, Integer submittedEvidenceVersion,
        String resultJson, String errorCode, String errorMessage, String adoptedAction
) {
    /** 从数据库实体转换为接口模型。 */
    public static DisputeAgentRunView from(DisputeAgentRunEntity run) {
        if (run == null) return null;
        return new DisputeAgentRunView(
                run.getRunId(), run.getStatus(), run.getSubmittedEvidenceVersion(),
                run.getResultJson(), run.getErrorCode(), run.getErrorMessage(), run.getAdoptedAction()
        );
    }
}
