package com.campus.trade.common.context;

import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;

import java.util.Optional;

/**
 * 当前请求用户上下文。
 *
 * <p>Tomcat 工作线程会被重复使用，因此请求结束后必须调用 clear，
 * 否则上一次请求的身份可能泄漏到下一次请求。</p>
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser currentUser) {
        CURRENT_USER.set(currentUser);
    }

    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    /**
     * 获取当前用户；正常情况下受保护接口在拦截器中已经完成认证。
     */
    public static CurrentUser requireCurrentUser() {
        return get().orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
