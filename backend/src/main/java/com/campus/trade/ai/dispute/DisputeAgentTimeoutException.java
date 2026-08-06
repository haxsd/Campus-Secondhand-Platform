package com.campus.trade.ai.dispute;

/** 模型调用超过请求时限的异常。 */
public class DisputeAgentTimeoutException extends RuntimeException {
    public DisputeAgentTimeoutException(String message, Throwable cause) { super(message, cause); }
}
