package com.campus.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis 连接参数。
 *
 * @param host           Redis 主机地址
 * @param port           Redis 服务端口
 * @param database       使用的逻辑数据库编号
 * @param password       Redis 密码，无密码时为空字符串
 * @param connectTimeout 建立连接的最长等待时间
 */
@ConfigurationProperties(prefix = "campus.redis")
public record RedisProperties(
        String host,
        int port,
        int database,
        String password,
        Duration connectTimeout
) {
}
