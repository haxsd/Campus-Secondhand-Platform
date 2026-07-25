package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 *
 * <p>DTO（Data Transfer Object）专门接收前端输入，不等同于数据库 User 实体。
 * 使用 record 可以得到不可变的数据对象，并自动生成 studentNo()、phone() 等读取方法。</p>
 *
 * @param studentNo 学号，只允许 4~32 位字母和数字
 * @param phone     中国大陆手机号
 * @param password  8~20 位且同时包含字母和数字
 * @param nickname  用户公开昵称
 * @param campus    所在校区
 */
public record RegisterRequest(
        // @NotBlank 拒绝 null、空字符串和纯空格；@Pattern 进一步限制实际格式。
        @NotBlank(message = "请输入学号")
        @Pattern(regexp = "^[A-Za-z0-9]{4,32}$", message = "学号格式不正确")
        String studentNo,

        // 与当前前端校验保持一致：1 开头、总长度 11 位。
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone,

        // 两个前瞻表达式分别要求至少一个字母和至少一个数字。
        @NotBlank(message = "请输入密码")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$",
                message = "密码 8~20 位，需同时包含字母和数字"
        )
        String password,

        // 数据库 nickname 为 VARCHAR(30)，DTO 必须在入库前执行相同上限校验。
        @NotBlank(message = "请输入昵称")
        @Size(max = 30, message = "昵称不能超过 30 个字符")
        String nickname,

        // 数据库 campus 为 VARCHAR(30)，避免超长输入到数据库阶段才报错。
        @NotBlank(message = "请选择校区")
        @Size(max = 30, message = "校区名称不能超过 30 个字符")
        String campus
) {
}
