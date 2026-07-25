package com.campus.trade.auth.jwt;

/**
 * 认证模块实际使用的 JWT 载荷。
 *
 * @param userId  用户 ID
 * @param role    用户角色
 * @param tokenId JWT 唯一标识 jti，用作 Redis 登录态键
 */
public record JwtClaims(Long userId, Integer role, String tokenId) {
}
