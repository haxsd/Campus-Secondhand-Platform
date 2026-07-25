package com.campus.trade.auth.service;

import com.campus.trade.auth.jwt.JwtClaims;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 登录态白名单。
 *
 * <p>JWT 自身负责防篡改和过期校验，Redis 负责主动失效。
 * 用户退出、封禁或修改密码时删除对应键，旧 JWT 即使尚未过期也无法继续使用。</p>
 */
@Service
public class LoginSessionService {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";

    private final RedissonClient redissonClient;

    public LoginSessionService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 保存登录态，TTL 必须与 JWT 有效期一致。
     */
    public void create(String tokenId, Long userId, Integer role, Duration ttl) {
        bucket(tokenId).set(sessionValue(userId, role), ttl);
    }

    /**
     * 同时校验键存在及其中用户信息一致，防止错误复用 jti。
     */
    public boolean isActive(JwtClaims claims) {
        String value = bucket(claims.tokenId()).get();
        return sessionValue(claims.userId(), claims.role()).equals(value);
    }

    /**
     * 退出登录时删除当前 token 对应的白名单键。
     */
    public void delete(String tokenId) {
        bucket(tokenId).delete();
    }

    private RBucket<String> bucket(String tokenId) {
        return redissonClient.getBucket(LOGIN_TOKEN_KEY_PREFIX + tokenId, StringCodec.INSTANCE);
    }

    private String sessionValue(Long userId, Integer role) {
        return userId + ":" + role;
    }
}
