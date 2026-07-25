package com.campus.trade.common.exception;

/**
 * 可预期的业务异常。
 *
 * <p>Service 遇到库存不足、状态冲突或越权等业务失败时抛出该异常，
 * 由全局异常处理器统一转换为 Result，避免 Controller 重复编写错误分支。</p>
 *
 * <p>它和程序 Bug 不同：例如“账号已注册”是正常业务分支，应使用 BizException；
 * NullPointerException 等未知错误则由全局兜底处理并记录完整日志。</p>
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        // 使用错误码枚举中的默认文案。
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BizException(ErrorCode errorCode, String message) {
        // 复用错误码语义，但允许业务给出更具体的文案。
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
