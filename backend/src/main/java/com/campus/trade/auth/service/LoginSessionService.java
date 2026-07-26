package com.campus.trade.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 使用 Redis 白名单管理 JWT 登录态。
 *
 * <p>JWT 负责证明令牌内容没有被篡改，Redis 只负责判断该令牌是否仍然有效。
 * Redis 键为 {@code login:token:{jti}}，值固定为 {@code 1}。
 * 登录态连续一天未访问会过期，每次有效访问都会重新获得一天有效期。</p>
 */
@Service
public class LoginSessionService {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    private static final String ACTIVE_VALUE = "1";
    private static final Duration SESSION_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public LoginSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 登录成功后保存 jti；连续一天没有访问后，Redis 会自动删除该登录态。 */
    public void create(String tokenId) {
        redisTemplate.opsForValue().set(key(tokenId), ACTIVE_VALUE, SESSION_TTL);
    }

    /**
     * 检查登录态并刷新空闲过期时间。
     *
     * <p>{@code getAndExpire} 使用一次 Redis 操作完成读取和续期。
     * key 不存在时返回 null，且不会重新创建已经退出或过期的登录态。</p>
     */
    public boolean isActive(String tokenId) {
        String value = redisTemplate.opsForValue().getAndExpire(key(tokenId), SESSION_TTL);
        // 登录态只以 key 是否存在为准，也兼容改造前 Redis 中遗留的旧格式值。
        return value != null;
    }

    /** 退出登录或修改密码后删除 jti，使尚未过期的 JWT 立即失效。 */
    public void delete(String tokenId) {
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        return LOGIN_TOKEN_KEY_PREFIX + tokenId;
    }
}
