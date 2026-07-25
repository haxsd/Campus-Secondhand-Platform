package com.campus.trade.common.exception;

/**
 * 可预期的业务异常。
 *
 * <p>Service 遇到库存不足、状态冲突或越权等业务失败时抛出该异常，
 * 由全局异常处理器统一转换为 Result，避免 Controller 重复编写错误分支。</p>
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
