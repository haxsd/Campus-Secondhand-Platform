package com.campus.trade.product.service;

import com.campus.trade.product.vo.ProductDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品公开详情的 Redis 缓存。
 *
 * <p>缓存不保存业务规则，只保存已经组装好的公开详情 JSON。商品主流程仍由 ProductService 和 MySQL
 * 决定；本类只负责降低热门详情页反复查询多张表的压力。</p>
 *
 * <p>缓存有三种状态：</p>
 * <ul>
 *     <li>未命中：Redis 没有该 key，需要查询 MySQL；</li>
 *     <li>详情命中：Redis 保存了完整 ProductDetailVO；</li>
 *     <li>空值命中：Redis 保存特殊标记，表示商品不存在或不可公开展示。</li>
 * </ul>
 * 空值缓存可避免攻击者反复请求不存在的 ID 时持续打到数据库。</p>
 */
@Service
public class ProductDetailCacheService {

    private static final String DETAIL_KEY_PREFIX = "product:detail:";
    private static final String NULL_KEY_PREFIX = "product:null:";
    private static final String NULL_VALUE = "__NULL_PRODUCT_DETAIL__";
    private static final Duration NULL_TTL = Duration.ofSeconds(10);

    /** 使用字符串模板，让缓存读写直接对应 Redis 的 GET、SET 和 DEL 命令。 */
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductDetailCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询 Redis 缓存。hit=false 才表示需要访问数据库；detail=null 且 hit=true 表示空值缓存命中。
     */
    public Lookup lookup(Long productId) {
        // 对应 Redis：GET product:detail:{id}。
        String detailJson = stringRedisTemplate.opsForValue().get(detailKey(productId));
        if (detailJson != null) {
            try {
                return new Lookup(true, objectMapper.readValue(detailJson, ProductDetailVO.class));
            } catch (JsonProcessingException exception) {
                // 缓存可能来自旧版本字段或意外损坏；删除后按未命中处理，不让缓存故障影响详情访问。
                stringRedisTemplate.delete(detailKey(productId));
            }
        }

        if (NULL_VALUE.equals(stringRedisTemplate.opsForValue().get(nullKey(productId)))) {
            return new Lookup(true, null);
        }
        return new Lookup(false, null);
    }

    /**
     * 缓存一件公开商品详情。基础 5 分钟上加入随机秒数，降低大量 key 同时过期造成的缓存雪崩风险。
     */
    public void putDetail(ProductDetailVO detail) {
        try {
            int randomSeconds = ThreadLocalRandom.current().nextInt(0, 31);
            // 对应 Redis：SET product:detail:{id} {json} EX {ttl}。
            stringRedisTemplate.opsForValue().set(
                    detailKey(detail.id()),
                    objectMapper.writeValueAsString(detail),
                    Duration.ofMinutes(5).plusSeconds(randomSeconds)
            );
        } catch (JsonProcessingException exception) {
            // 缓存写入失败不应阻断正常商品详情响应；下一次请求会再次从数据库读取。
        }
    }

    /**
     * 缓存不存在或不可公开商品的结果，TTL 很短，避免商品刚审核通过后长时间仍被判定不存在。
     */
    public void putNull(Long productId) {
        // 对应 Redis：SET product:null:{id} __NULL_PRODUCT_DETAIL__ EX 10。
        stringRedisTemplate.opsForValue().set(nullKey(productId), NULL_VALUE, NULL_TTL);
    }

    /**
     * 删除正常详情缓存和空值缓存。写操作提交后调用，保证后续读取能看到最新状态。
     */
    public void invalidate(Long productId) {
        // DEL 支持一次传入多个 key，正常详情缓存和空值缓存必须同时失效。
        stringRedisTemplate.delete(java.util.List.of(detailKey(productId), nullKey(productId)));
    }

    /** 构造正常商品详情缓存键。 */
    private String detailKey(Long productId) {
        return DETAIL_KEY_PREFIX + productId;
    }

    /** 构造“不存在/不可公开”商品的短期空值缓存键。 */
    private String nullKey(Long productId) {
        return NULL_KEY_PREFIX + productId;
    }

    /**
     * 缓存查询结果。detail 为 null 时必须结合 hit 判断，不能把它简单理解为缓存未命中。
     */
    public record Lookup(boolean hit, ProductDetailVO detail) {
    }
}
