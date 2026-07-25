package com.campus.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis 连接参数。
 *
 * <p>配置来源是 application-dev.yml 的 {@code campus.redis}。
 * 使用专用配置对象比在多个类中反复写 {@code @Value} 更集中、更容易测试。</p>
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
