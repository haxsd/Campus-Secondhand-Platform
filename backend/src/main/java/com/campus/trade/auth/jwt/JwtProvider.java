package com.campus.trade.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 的签发与验签组件。
 *
 * <p>这里只处理 token 的密码学有效性和载荷解析；token 是否已退出、
 * 是否被服务端主动失效，由 LoginSessionService 查询 Redis 决定。</p>
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("campus.jwt.secret 至少需要 32 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * 为登录成功的用户签发带 jti 的 JWT。
     */
    public IssuedToken issue(Long userId, Integer role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.ttl());
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .id(tokenId)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new IssuedToken(token, tokenId, properties.ttl());
    }

    /**
     * 验证签名、签发者和过期时间，并提取业务需要的载荷。
     *
     * <p>非法、被篡改或过期 token 会由 JJWT 抛出 JwtException，
     * 登录拦截器统一转换为 401，不向前端暴露具体失败原因。</p>
     */
    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        Integer role = claims.get(ROLE_CLAIM, Integer.class);
        return new JwtClaims(userId, role, claims.getId());
    }
}
