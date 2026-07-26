package com.campus.trade.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 使用 Redis 白名单管理 JWT 登录态。
 *
 * <p>JWT 负责证明令牌内容没有被篡改，Redis 只负责判断该令牌是否仍然有效。
 * Redis 键为 {@code login:token:{jti}}，值固定为 {@code 1}，过期时间与 JWT 一致。</p>
 */
@Service
public class LoginSessionService {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    private static final String ACTIVE_VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    public LoginSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 登录成功后保存 jti，并设置与 JWT 相同的有效期。 */
    public void create(String tokenId, Duration ttl) {
        redisTemplate.opsForValue().set(key(tokenId), ACTIVE_VALUE, ttl);
    }

    /** Redis 中仍存在该 jti，说明当前登录态有效。 */
    public boolean isActive(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
    }

    /** 退出登录或修改密码后删除 jti，使尚未过期的 JWT 立即失效。 */
    public void delete(String tokenId) {
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        return LOGIN_TOKEN_KEY_PREFIX + tokenId;
    }
}
