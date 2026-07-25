package com.campus.trade.auth.vo;

/**
 * 登录成功时返回的精简用户信息。
 *
 * @param id       用户 ID
 * @param nickname 昵称
 * @param avatar   头像地址
 * @param campus   所在校区
 * @param role     角色：0 普通用户，1 管理员
 */
public record LoginUserVO(
        Long id,
        String nickname,
        String avatar,
        String campus,
        Integer role
) {
}
