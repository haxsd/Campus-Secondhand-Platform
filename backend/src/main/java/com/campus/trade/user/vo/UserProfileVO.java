package com.campus.trade.user.vo;

/**
 * 当前用户个人中心展示数据。
 *
 * <p>该 VO 只会返回给当前已认证用户，因此可包含手机号；密码哈希、账号状态等内部字段
 * 仍然不允许离开 Service 层。</p>
 *
 * @param id        用户 ID
 * @param studentNo 学号（仅展示，不可修改）
 * @param phone     手机号（仅展示，不可修改）
 * @param nickname  昵称
 * @param avatar    头像地址
 * @param campus    所在校区
 * @param role      用户角色
 */
public record UserProfileVO(
        Long id,
        String studentNo,
        String phone,
        String nickname,
        String avatar,
        String campus,
        Integer role
) {
}
