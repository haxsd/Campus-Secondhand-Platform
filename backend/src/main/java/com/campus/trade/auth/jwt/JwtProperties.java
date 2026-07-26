package com.campus.trade.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 签发配置。
 *
 * <p>{@code @ConfigurationProperties} 会把 application-dev.yml 中的
 * {@code campus.jwt.*} 自动绑定到该 record，避免在业务代码中散落字符串配置名。</p>
 *
 * @param issuer  签发者，用于拒绝其他系统签发的 token
 * @param secret  HMAC 密钥，HS256 至少需要 32 字节
 * @param ttl     JWT 最长有效期
 */
@ConfigurationProperties(prefix = "campus.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration ttl
) {
}
