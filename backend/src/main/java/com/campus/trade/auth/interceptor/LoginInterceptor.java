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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 登录认证拦截器。
 *
 * <p>验证顺序为 Bearer 请求头 → JWT 签名与过期时间 → Redis 白名单。
 * 三步全部通过后才把用户身份写入 UserContext。公开接口采用“可选认证”：
 * 没有 token 允许匿名访问；携带合法 token 时仍会识别用户身份。</p>
 *
 * <p>拦截器运行在 Controller 之前，适合处理所有受保护接口都需要执行的认证逻辑，
 * 避免每个 Controller 都重复编写“取 token、验签、查 Redis”。</p>
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
        // PublicRequestMatcher 判断接口是否允许匿名访问。
        this.publicRequestMatcher = publicRequestMatcher;
        // JwtProvider 验证 token 是否由本系统签发、是否被篡改以及是否过期。
        this.jwtProvider = jwtProvider;
        // LoginSessionService 检查 token 是否仍在 Redis 白名单中。
        this.loginSessionService = loginSessionService;
        // 拦截器不能直接 return Result，因此使用 ObjectMapper 手动输出 JSON。
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws IOException {
        // 第 1 步：先判断接口是否公开。公开不代表永远跳过认证，
        // 它只表示“没有 token 时也允许继续”；商品详情需要借此识别已登录用户并记录浏览历史。
        boolean publicRequest = publicRequestMatcher.isPublic(request);

        // 第 2 步：读取约定的 Authorization 请求头。
        // 正确格式为：Authorization: Bearer eyJhbGciOi...
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || header.isBlank()) {
            // 公开请求没有 token，按匿名访问处理；受保护请求仍必须登录。
            if (publicRequest) {
                return true;
            }
            writeFailure(response, ErrorCode.UNAUTHORIZED);
            return false;
        }
        if (!header.startsWith(BEARER_PREFIX)) {
            // 已携带认证头但格式错误时统一返回 401，避免把错误 token 当匿名请求悄悄放过。
            writeFailure(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        try {
            // 第 3 步：去掉 Bearer 前缀，只保留 JWT 字符串。
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (token.isEmpty()) {
                writeFailure(response, ErrorCode.UNAUTHORIZED);
                return false;
            }

            // 第 4 步：验签、检查签发者和过期时间，并解析 userId、role、jti。
            JwtClaims claims = jwtProvider.parse(token);

            // 第 5 步：查询 Redis 白名单。
            // JWT 合法但 Redis key 已删除时，说明用户已经退出或 token 被主动吊销。
            if (!loginSessionService.isActive(claims)) {
                writeFailure(response, ErrorCode.UNAUTHORIZED);
                return false;
            }

            // 第 6 步：把认证结果放入当前线程。
            // 后续 Controller 和 Service 不需要再次解析 token，直接从 UserContext 获取身份；
            // 对公开详情接口而言，这一步让 BrowseHistoryService 知道本次是否需要写浏览记录。
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
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @NonNull Exception exception
    ) {
        // 无论业务成功还是抛异常，都必须清理 ThreadLocal，防止线程复用造成串号。
        UserContext.clear();
    }

    private void writeFailure(@NonNull HttpServletResponse response, @NonNull ErrorCode errorCode) throws IOException {
        // 项目 API 契约约定业务错误写在 body.code 中，因此 HTTP 状态仍返回 200。
        response.setStatus(HttpServletResponse.SC_OK);
        // 明确 UTF-8，避免中文错误信息在浏览器中乱码。
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                Result.fail(errorCode.getCode(), errorCode.getMessage())
        );
    }
}
