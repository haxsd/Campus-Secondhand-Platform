package com.campus.trade.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户更新个人资料的请求对象。
 *
 * <p>只保留允许用户自行修改的字段。学号、手机号、角色等字段没有出现在 DTO 中，
 * 这样 Controller 即使直接接收前端 JSON，也不会产生批量赋值漏洞。</p>
 *
 * @param nickname 用户公开昵称
 * @param campus   当前所在校区
 * @param avatar   已上传头像的访问地址；传空字符串可清除头像
 */
public record UpdateProfileRequest(
        @NotBlank(message = "请输入昵称")
        @Size(max = 30, message = "昵称不能超过 30 个字符")
        String nickname,

        @NotBlank(message = "请选择校区")
        @Size(max = 30, message = "校区名称不能超过 30 个字符")
        String campus,

        @Size(max = 255, message = "头像地址不能超过 255 个字符")
        String avatar
) {
}
