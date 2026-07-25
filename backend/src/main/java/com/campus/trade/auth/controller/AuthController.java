package com.campus.trade.auth.controller;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.vo.CurrentUserVO;
import com.campus.trade.auth.vo.LoginResponse;
import com.campus.trade.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口。
 *
 * <p>Controller 只负责接收和校验参数，密码处理、事务和登录态均由 AuthService 完成。</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<CurrentUserVO> currentUser() {
        return Result.ok(authService.currentUser());
    }
}
