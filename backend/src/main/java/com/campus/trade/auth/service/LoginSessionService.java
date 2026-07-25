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
 *
 * <p>Redis 中的数据示例：</p>
 * <pre>
 * key   = login:token:96d58af0-...
 * value = 15:0                 // userId:role
 * ttl   = 7 天
 * </pre>
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
        // RBucket 可以理解成 Redis 中一个最简单的 key-value。
        // set(value, ttl) 在写入值的同时设置过期时间，避免失效 token 永久占用内存。
        bucket(tokenId).set(sessionValue(userId, role), ttl);
    }

    /**
     * 同时校验键存在及其中用户信息一致，防止错误复用 jti。
     */
    public boolean isActive(JwtClaims claims) {
        // Redis 中不存在该 jti，说明 token 已退出、已被主动失效或 TTL 已到期。
        String value = bucket(claims.tokenId()).get();

        // 不只检查 key 存在，还核对 userId 和 role，防止错误的 jti 关联被接受。
        return sessionValue(claims.userId(), claims.role()).equals(value);
    }

    /**
     * 退出登录时删除当前 token 对应的白名单键。
     */
    public void delete(String tokenId) {
        // delete 是幂等的：key 已不存在时再次删除也不会产生副作用。
        bucket(tokenId).delete();
    }

    private RBucket<String> bucket(String tokenId) {
        // StringCodec 保证 Redis 中看到的是普通字符串，便于使用 redis-cli 排查。
        return redissonClient.getBucket(LOGIN_TOKEN_KEY_PREFIX + tokenId, StringCodec.INSTANCE);
    }

    private String sessionValue(Long userId, Integer role) {
        return userId + ":" + role;
    }
}
