package com.campus.trade.ai.dispute;

/** 模型返回无法通过结构化校验时使用的专用异常。 */
public class DisputeAgentOutputInvalidException extends RuntimeException {
    private final String rawResponse;

    public DisputeAgentOutputInvalidException(String message) {
        this(message, null, null);
    }

    public DisputeAgentOutputInvalidException(String message, Throwable cause) {
        this(message, cause, null);
    }

    /** 保存失败时模型返回的原文，便于管理员通过 run 定位问题。 */
    public DisputeAgentOutputInvalidException(
            String message,
            Throwable cause,
            String rawResponse
    ) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
