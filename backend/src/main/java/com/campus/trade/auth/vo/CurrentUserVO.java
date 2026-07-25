package com.campus.trade.auth.vo;

/**
 * 当前登录用户信息。
 *
 * @param id        用户 ID
 * @param studentNo 学号
 * @param nickname  昵称
 * @param avatar    头像地址
 * @param campus    所在校区
 * @param role      角色编码
 */
public record CurrentUserVO(
        Long id,
        String studentNo,
        String nickname,
        String avatar,
        String campus,
        Integer role
) {
}
