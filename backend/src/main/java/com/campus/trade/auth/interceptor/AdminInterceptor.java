package com.campus.trade.auth.interceptor;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.Result;
import com.campus.trade.user.model.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 管理员权限拦截器。
 *
 * <p>它只注册到 /admin/**，并且执行顺序晚于 LoginInterceptor，
 * 因此可以直接从 UserContext 读取已经认证的用户角色。</p>
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AdminInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        boolean isAdmin = UserContext.get()
                .map(user -> user.role() == UserRole.ADMIN.getCode())
                .orElse(false);
        if (isAdmin) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                Result.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage())
        );
        return false;
    }
}
