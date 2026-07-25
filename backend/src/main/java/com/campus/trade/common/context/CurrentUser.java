package com.campus.trade.common.context;

/**
 * 当前请求通过认证后的用户身份。
 *
 * <p>这是后端内部对象，不是数据库 User，也不会直接返回前端。
 * 它只保存一次请求做权限判断所需的最小身份信息。</p>
 *
 * @param userId  用户 ID
 * @param role    角色编码
 * @param tokenId 当前 JWT 的 jti，退出登录时用于删除 Redis 登录态
 */
public record CurrentUser(Long userId, Integer role, String tokenId) {
}
