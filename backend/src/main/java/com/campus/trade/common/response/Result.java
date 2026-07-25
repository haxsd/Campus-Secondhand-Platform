package com.campus.trade.common.response;

/**
 * 全部 HTTP 接口统一使用的响应结构。
 *
 * <p>前端响应拦截器依赖 {@code code/message/data} 三个字段：
 * code 为 0 时读取 data，其他值统一作为业务失败处理。</p>
 *
 * <p>record 很适合只承载数据的响应对象：字段不可变，并自动生成
 * {@code code()}、{@code message()}、{@code data()} 等读取方法。</p>
 *
 * @param code    业务状态码，0 表示成功
 * @param message 面向用户的简短提示
 * @param data    成功时返回的业务数据，无数据时为 null
 * @param <T>     业务数据类型
 */
public record Result<T>(int code, String message, T data) {

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MESSAGE = "ok";

    /**
     * 构造带业务数据的成功响应。
     *
     * @param data 返回给前端的数据
     * @param <T>  数据类型
     * @return 统一成功响应
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 构造没有业务数据的成功响应。
     *
     * @return data 为 null 的统一成功响应
     */
    public static Result<Void> ok() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 构造业务失败响应。
     *
     * @param code    与 API 文档一致的业务错误码
     * @param message 可直接展示给用户的错误信息
     * @return data 为 null 的统一失败响应
     */
    public static Result<Void> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
