package com.campus.trade.auth.interceptor;

import com.campus.trade.auth.jwt.JwtClaims;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.service.LoginSessionService;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录认证拦截器。
 *
 * <p>验证顺序为 Bearer 请求头 → JWT 签名与过期时间 → Redis 白名单。
 * 三步全部通过后才把用户身份写入 UserContext。</p>
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PublicRequestMatcher publicRequestMatcher;
    private final JwtProvider jwtProvider;
    private final LoginSessionService loginSessionService;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(
            PublicRequestMatcher publicRequestMatcher,
            JwtProvider jwtProvider,
            LoginSessionService loginSessionService,
            ObjectMapper objectMapper
    ) {
        this.publicRequestMatcher = publicRequestMatcher;
        this.jwtProvider = jwtProvider;
        this.loginSessionService = loginSessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (publicRequestMatcher.isPublic(request)) {
            return true;
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeFailure(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        try {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                writeFailure(response, ErrorCode.UNAUTHORIZED);
                return false;
            }

            JwtClaims claims = jwtProvider.parse(token);
            if (!loginSessionService.isActive(claims)) {
                writeFailure(response, ErrorCode.UNAUTHORIZED);
                return false;
            }

            UserContext.set(new CurrentUser(claims.userId(), claims.role(), claims.tokenId()));
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            // 对外统一表现为 token 失效，不区分过期、签名错误或载荷非法。
            writeFailure(response, ErrorCode.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        // 无论业务成功还是抛异常，都必须清理 ThreadLocal，防止线程复用造成串号。
        UserContext.clear();
    }

    private void writeFailure(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                Result.fail(errorCode.getCode(), errorCode.getMessage())
        );
    }
}
