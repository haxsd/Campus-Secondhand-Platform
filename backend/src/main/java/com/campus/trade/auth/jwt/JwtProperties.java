package com.campus.trade.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 签发配置。
 *
 * @param issuer  签发者，用于拒绝其他系统签发的 token
 * @param secret  HMAC 密钥，HS256 至少需要 32 字节
 * @param ttl     token 与 Redis 登录态的有效期
 */
@ConfigurationProperties(prefix = "campus.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration ttl
) {
}
