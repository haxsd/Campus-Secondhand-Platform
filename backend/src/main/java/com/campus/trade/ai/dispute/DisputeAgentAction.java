package com.campus.trade.ai.dispute;

/** AI 可以提出但不能直接执行的纠纷处理建议。 */
public enum DisputeAgentAction {
    REJECT, KEEP_COMPLETED, CANCEL_TRADE, NEED_MORE
}
