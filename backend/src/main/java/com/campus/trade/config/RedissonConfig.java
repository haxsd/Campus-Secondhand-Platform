package com.campus.trade.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 客户端配置。
 *
 * <p>Redisson 与 Spring Data Redis 共用 {@code spring.data.redis} 的连接参数，避免为了
 * 一个缓存重建锁再维护第二套 Redis 地址、密码和数据库编号。当前项目只使用单节点 Redis；
 * 将来切换 Redis Cluster 时，再针对实际部署方式调整本类即可。</p>
 */
@Configuration
public class RedissonConfig {

    /**
     * 创建用于分布式锁的 Redisson 客户端。
     *
     * <p>这里不把锁封装成通用框架：目前唯一用途是商品详情缓存重建，调用方仍能明确看到
     * “缓存未命中 -> 获取商品粒度锁 -> 回源”的完整流程。</p>
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        var singleServer = config.useSingleServer()
                // Redisson 单节点地址必须带 redis:// 协议前缀。
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase());

        // 空密码表示 Redis 未开启认证，不能把空字符串作为 AUTH 密码发送。
        if (StringUtils.hasText(redisProperties.getPassword())) {
            singleServer.setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }
}
