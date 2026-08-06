package com.campus.trade.ai.dispute;

/** 模型服务暂时不可用，可以按策略重试的异常。 */
public class DisputeAgentRetryableException extends RuntimeException {
    public DisputeAgentRetryableException(String message) { super(message); }
    public DisputeAgentRetryableException(String message, Throwable cause) { super(message, cause); }
}
