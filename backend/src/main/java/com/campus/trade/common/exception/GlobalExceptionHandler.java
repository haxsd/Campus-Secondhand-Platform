package com.campus.trade.common.exception;

import com.campus.trade.common.response.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * REST 接口的统一异常出口。
 *
 * <p>项目约定 HTTP 层保持成功响应，业务结果通过响应体 code 判断。
 * 未知异常只向前端返回通用文案，完整堆栈写入服务端日志，防止泄露实现细节。</p>
 *
 * <p>{@code @RestControllerAdvice} 会监控所有 Controller 抛出的异常，
 * 并根据异常类型选择最匹配的 {@code @ExceptionHandler} 方法。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Service 主动抛出的可预期业务异常。
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        // 业务异常可以安全地把预先设计好的 code 和 message 返回给前端。
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理 @Valid 校验 JSON 请求体时产生的字段错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), firstFieldError(exception.getBindingResult()));
    }

    /**
     * 处理表单对象绑定错误。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), firstFieldError(exception.getBindingResult()));
    }

    /**
     * 处理方法参数上的约束，例如 @Min、@NotBlank。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理缺少查询参数或 JSON 格式错误。
     */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleMalformedRequest(Exception exception) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getMessage());
    }

    /**
     * 兜底处理未预期异常。日志保留完整上下文，响应不返回数据库或代码细节。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknownException(Exception exception) {
        // 服务端日志保留堆栈供开发者定位，但响应不返回 SQL、类名等内部细节。
        log.error("未处理的服务端异常", exception);
        return Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private String firstFieldError(org.springframework.validation.BindingResult bindingResult) {
        // 一个请求可能同时有多个字段错误，第一版只返回第一条，避免提示过于冗长。
        return bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
    }
}
