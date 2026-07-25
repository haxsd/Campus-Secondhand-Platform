package com.campus.trade.common.exception;

/**
 * 全局通用错误码。
 *
 * <p>业务模块可以复用这些通用语义；如果后续需要更细的内部编号，
 * 仍需保证对外 code 与 API 文档中的 400/401/403/404/409/429/500 一致。</p>
 */
public enum ErrorCode {

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "请求的资源不存在"),
    CONFLICT(409, "当前状态不允许此操作"),
    TOO_MANY_REQUESTS(429, "操作太频繁，请稍后重试"),
    INTERNAL_ERROR(500, "服务器繁忙，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
