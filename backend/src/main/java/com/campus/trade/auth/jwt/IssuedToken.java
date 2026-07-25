package com.campus.trade.auth.jwt;

import java.time.Duration;

/**
 * 刚签发的 token 及其 Redis 登录态所需元数据。
 *
 * @param value   JWT 字符串
 * @param tokenId JWT 唯一标识
 * @param ttl     剩余有效期
 */
public record IssuedToken(String value, String tokenId, Duration ttl) {
}
