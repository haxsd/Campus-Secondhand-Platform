package com.campus.trade.auth.controller;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.vo.LoginResponse;
import com.campus.trade.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口。
 *
 * <p>可以把 Controller 理解成后端的“HTTP 接待层”：</p>
 * <ol>
 *     <li>根据请求路径找到对应方法；</li>
 *     <li>把 JSON 请求体转换成 DTO；</li>
 *     <li>使用 {@code @Valid} 执行参数校验；</li>
 *     <li>调用 Service 完成真正的业务；</li>
 *     <li>用 {@link Result} 包装结果后返回给前端。</li>
 * </ol>
 *
 * <p>密码加密、数据库事务、JWT 和 Redis 登录态都不应该写在 Controller 中，
 * 否则 HTTP 处理与业务逻辑会混在一起，难以复用和测试。</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        // 使用构造器注入：依赖明确、便于测试，也避免字段注入隐藏依赖关系。
        this.authService = authService;
    }

    /**
     * 注册普通用户。
     *
     * <p>{@code @RequestBody} 把前端 JSON 转为 RegisterRequest；
     * {@code @Valid} 在进入 Service 之前检查学号、手机号和密码格式。</p>
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        // API 契约规定注册成功不返回额外数据，因此 data 为 null。
        return Result.ok();
    }

    /**
     * 使用学号或手机号登录。
     *
     * <p>成功后 data 中包含 JWT 和前端展示所需的安全用户信息。</p>
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 退出当前登录。
     *
     * <p>用户身份已经由 LoginInterceptor 放入 UserContext，
     * Service 会删除当前 JWT 的 Redis 白名单记录。</p>
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

}
