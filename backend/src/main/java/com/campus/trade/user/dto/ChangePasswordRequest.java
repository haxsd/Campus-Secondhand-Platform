package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 修改密码请求。
 *
 * @param oldPassword 当前密码，用于证明操作者仍掌握原账号凭证
 * @param newPassword 新密码，规则与注册时保持一致
 */
public record ChangePasswordRequest(
        @NotBlank(message = "请输入当前密码")
        String oldPassword,

        @NotBlank(message = "请输入新密码")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$",
                message = "密码 8~20 位，需同时包含字母和数字"
        )
        String newPassword
) {
}
