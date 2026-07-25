package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求。
 *
 * <p>登录接口只需要账号和密码。它不会接收 userId 或 role，
 * 因为用户身份和角色只能由后端根据数据库记录确定。</p>
 *
 * @param account  学号或手机号
 * @param password 明文密码，仅用于本次 BCrypt 校验，不记录日志
 */
public record LoginRequest(
        // account 可以是学号或手机号，具体匹配逻辑由 UserMapper 完成。
        @NotBlank(message = "请输入学号或手机号")
        @Size(max = 32, message = "账号格式不正确")
        String account,

        // 登录使用与注册相同的最大长度，异常提示不泄露账号是否存在。
        @NotBlank(message = "请输入密码")
        @Size(max = 20, message = "账号或密码错误")
        String password
) {
}
