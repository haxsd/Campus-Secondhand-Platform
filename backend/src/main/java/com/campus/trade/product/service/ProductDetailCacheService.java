package com.campus.trade.product.service;

import com.campus.trade.product.vo.ProductDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品公开详情缓存。
 *
 * <p>每个商品只使用一个键 {@code product:detail:{id}}：正常商品保存详情 JSON，
 * 不存在或不可公开的商品保存空值标记，Redis 中没有该键才表示缓存未命中。</p>
 */
@Service
public class ProductDetailCacheService {

    private static final String KEY_PREFIX = "product:detail:";
    private static final String NULL_VALUE = "__NULL_PRODUCT_DETAIL__";
    private static final Duration NULL_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductDetailCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /** 查询缓存；hit=true 且 detail=null 表示命中了空值缓存。 */
    public Lookup lookup(Long productId) {
        String cachedValue = redisTemplate.opsForValue().get(key(productId));
        if (cachedValue == null) {
            return new Lookup(false, null);
        }
        if (NULL_VALUE.equals(cachedValue)) {
            return new Lookup(true, null);
        }

        try {
            return new Lookup(true, objectMapper.readValue(cachedValue, ProductDetailVO.class));
        } catch (JsonProcessingException exception) {
            // 旧版本或损坏的缓存直接删除，下次请求重新从数据库加载。
            redisTemplate.delete(key(productId));
            return new Lookup(false, null);
        }
    }

    /** 缓存正常详情，随机增加少量 TTL，避免大量缓存同时过期。 */
    public void putDetail(ProductDetailVO detail) {
        try {
            Duration ttl = Duration.ofMinutes(5)
                    .plusSeconds(ThreadLocalRandom.current().nextInt(0, 31));
            redisTemplate.opsForValue().set(
                    key(detail.id()),
                    objectMapper.writeValueAsString(detail),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            // 缓存失败不影响商品详情的正常返回。
        }
    }

    /** 短暂缓存不存在或不可公开的商品，减少无效请求反复查询数据库。 */
    public void putNull(Long productId) {
        redisTemplate.opsForValue().set(key(productId), NULL_VALUE, NULL_TTL);
    }

    /** 商品数据变化后删除详情缓存。 */
    public void invalidate(Long productId) {
        redisTemplate.delete(key(productId));
    }

    private String key(Long productId) {
        return KEY_PREFIX + productId;
    }

    public record Lookup(boolean hit, ProductDetailVO detail) {
    }
}
