package com.campus.trade.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.Credentials;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

/**
 * Redisson 客户端配置。
 *
 * <p>第一版为本机单节点 Redis。以后切换哨兵或集群时，只需要调整本配置，
 * 业务代码继续依赖 RedissonClient，不感知部署方式。</p>
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonConfig {

    /**
     * 创建全局唯一的 RedissonClient，并在 Spring 容器关闭时释放连接。
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties properties) {
        // Config 是 Redisson 的客户端配置对象。
        Config config = new Config();

        // Redisson 地址必须包含 redis:// 协议前缀；TLS 连接则使用 rediss://。
        String address = "redis://%s:%d".formatted(properties.host(), properties.port());

        // 当前 Docker 环境是单节点 Redis，因此选择 useSingleServer。
        var serverConfig = config.useSingleServer()
                .setAddress(address)
                // database=0 表示 Redis 第 0 号逻辑数据库。
                .setDatabase(properties.database())
                // 连接超时转成 Redisson API 需要的毫秒整数。
                .setConnectTimeout(Math.toIntExact(properties.connectTimeout().toMillis()));

        // 本地 Redis 当前无密码；生产环境配置密码后动态提供认证信息。
        if (StringUtils.hasText(properties.password())) {
            config.setCredentialsResolver(nodeAddress ->
                    CompletableFuture.completedFuture(new Credentials(null, properties.password()))
            );
        }

        // Redisson.create 会建立连接池；destroyMethod=shutdown 会在应用关闭时释放资源。
        return Redisson.create(config);
    }
}
