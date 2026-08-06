package com.campus.trade.ai.dispute;

/** 模型返回无法通过结构化校验时使用的专用异常。 */
public class DisputeAgentOutputInvalidException extends RuntimeException {
    public DisputeAgentOutputInvalidException(String message) { super(message); }
    public DisputeAgentOutputInvalidException(String message, Throwable cause) { super(message, cause); }
}
