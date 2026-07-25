package com.campus.trade.auth.jwt;

/**
 * 认证模块实际使用的 JWT 载荷。
 *
 * <p>JwtProvider 从第三方库的通用 Claims 中只提取本项目需要的三个字段，
 * 后续代码就不必依赖字符串 key，也不会误用 JWT 中的其他内容。</p>
 *
 * @param userId  用户 ID
 * @param role    用户角色
 * @param tokenId JWT 唯一标识 jti，用作 Redis 登录态键
 */
public record JwtClaims(Long userId, Integer role, String tokenId) {
}
