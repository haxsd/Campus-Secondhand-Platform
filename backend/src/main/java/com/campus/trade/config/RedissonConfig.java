package com.campus.trade.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

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
        Config config = new Config();
        String address = "redis://%s:%d".formatted(properties.host(), properties.port());

        var serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.database())
                .setConnectTimeout(Math.toIntExact(properties.connectTimeout().toMillis()));

        // 本地 Redis 当前无密码；生产环境配置密码后才调用 setPassword。
        if (StringUtils.hasText(properties.password())) {
            serverConfig.setPassword(properties.password());
        }

        return Redisson.create(config);
    }
}
