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
 *
 * <p>JWT 可以理解成服务端签名的“临时身份证”。它由三段字符串组成：
 * Header（算法）、Payload（载荷）和 Signature（签名）。Payload 可以被读取，
 * 因此不能放密码等秘密；Signature 用来发现内容是否被篡改。</p>
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties properties) {
        this.properties = properties;

        // HS256 要求密钥至少 256 bit，也就是 32 字节。过短密钥会降低签名安全性。
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
        // 所有时间都先使用 Instant，避免服务器时区影响 token 过期判断。
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.ttl());

        // jti 是“这一次登录”的唯一编号；同一用户多次登录会得到不同 jti。
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                // iss：签发者。解析时强制检查，拒绝其他系统签发的 token。
                .issuer(properties.issuer())
                // sub：JWT 标准主题字段，这里存用户 ID。
                .subject(userId.toString())
                // jti：JWT 唯一编号，也是 Redis 登录态 key 的一部分。
                .id(tokenId)
                // role 是项目自定义载荷，用于管理员权限判断。
                .claim(ROLE_CLAIM, role)
                // iat 与 exp 分别代表签发时间和过期时间。
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                // 使用构造器中生成的 HMAC 密钥进行签名，防止载荷被篡改。
                .signWith(secretKey)
                .compact();

        // AuthService 还需要 tokenId 创建 Redis 登录态，因此与 JWT 字符串一起返回。
        return new IssuedToken(token, tokenId);
    }

    /**
     * 验证签名、签发者和过期时间，并提取业务需要的载荷。
     *
     * <p>非法、被篡改或过期 token 会由 JJWT 抛出 JwtException，
     * 登录拦截器统一转换为 401，不向前端暴露具体失败原因。</p>
     */
    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                // verifyWith 会验证签名；任何一个字符被修改都会验签失败。
                .verifyWith(secretKey)
                // 除了签名，还要求 iss 必须是本系统。
                .requireIssuer(properties.issuer())
                .build()
                // parseSignedClaims 同时会检查 exp，过期 token 会直接抛出异常。
                .parseSignedClaims(token)
                .getPayload();

        // 将第三方库的通用 Claims 转换为项目自己的、类型明确的 JwtClaims。
        Long userId = Long.valueOf(claims.getSubject());
        Integer role = claims.get(ROLE_CLAIM, Integer.class);
        return new JwtClaims(userId, role, claims.getId());
    }
}
