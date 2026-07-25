package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求。
 *
 * @param account  学号或手机号
 * @param password 明文密码，仅用于本次 BCrypt 校验，不记录日志
 */
public record LoginRequest(
        @NotBlank(message = "请输入学号或手机号")
        @Size(max = 32, message = "账号格式不正确")
        String account,

        @NotBlank(message = "请输入密码")
        @Size(max = 20, message = "账号或密码错误")
        String password
) {
}
