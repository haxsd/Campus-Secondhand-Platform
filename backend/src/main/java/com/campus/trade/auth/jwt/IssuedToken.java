package com.campus.trade.auth.jwt;

/**
 * 刚签发的 JWT 及其唯一编号。
 *
 * <p>前端只会收到 value；tokenId 用于创建 Redis 登录态。</p>
 *
 * @param value   JWT 字符串
 * @param tokenId JWT 唯一标识
 */
public record IssuedToken(String value, String tokenId) {
}
