package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 *
 * @param studentNo 学号，只允许 4~32 位字母和数字
 * @param phone     中国大陆手机号
 * @param password  8~20 位且同时包含字母和数字
 * @param nickname  用户公开昵称
 * @param campus    所在校区
 */
public record RegisterRequest(
        @NotBlank(message = "请输入学号")
        @Pattern(regexp = "^[A-Za-z0-9]{4,32}$", message = "学号格式不正确")
        String studentNo,

        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "请输入密码")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$",
                message = "密码 8~20 位，需同时包含字母和数字"
        )
        String password,

        @NotBlank(message = "请输入昵称")
        @Size(max = 30, message = "昵称不能超过 30 个字符")
        String nickname,

        @NotBlank(message = "请选择校区")
        @Size(max = 30, message = "校区名称不能超过 30 个字符")
        String campus
) {
}
