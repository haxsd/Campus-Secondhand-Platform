package com.campus.trade.product.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.campus.trade.product.vo.ProductDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 商品公开详情缓存。
 *
 * <p>读取顺序为“Caffeine 本机一级缓存 → Redis 二级缓存 → MySQL”。Caffeine 只保存正常公开详情，
 * TTL 必须短于 Redis，避免多实例部署时某个实例长期保留旧数据；空值标记仍只保存在 Redis，
 * 防止本机短时间缓存不存在商品而影响其他实例的真实回填。</p>
 *
 * <p>Redis 中每个商品只使用一个键 {@code product:detail:{id}}：正常商品保存详情 JSON，
 * 不存在或不可公开的商品保存空值标记，Redis 中没有该键才表示缓存未命中。</p>
 */
@Service
public class ProductDetailCacheService {

    private static final String KEY_PREFIX = "product:detail:";
    private static final String REBUILD_LOCK_KEY_PREFIX = "lock:product:detail:";
    private static final String NULL_VALUE = "__NULL_PRODUCT_DETAIL__";
    private static final Duration NULL_TTL = Duration.ofSeconds(10);
    /**
     * 本机缓存只作为短暂的读优化，不是跨实例事实源。
     * Redis 正常详情 TTL 约为 5 分钟，因此一级缓存固定为 60 秒且限制容量，
     * 即使某实例漏掉失效动作，最多也只会在这一小段时间内读到旧详情。
     */
    private static final Duration LOCAL_DETAIL_TTL = Duration.ofSeconds(60);
    private static final long LOCAL_DETAIL_MAXIMUM_SIZE = 1_000L;
    /** 等待时间有上限，不能为了缓存而长期占用 Web 请求线程。 */
    private static final long REBUILD_LOCK_WAIT_MILLIS = 200L;
    /** 商品详情由少量本地 SQL 组成，10 秒租约足够覆盖正常回源且能避免异常死锁。 */
    private static final long REBUILD_LOCK_LEASE_MILLIS = 10_000L;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final Cache<Long, ProductDetailVO> localDetailCache;

    public ProductDetailCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedissonClient redissonClient
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redissonClient = redissonClient;
        // 每个应用实例独立维护这份短 TTL 缓存，不需要也不能在这里共享到 Redis。
        this.localDetailCache = Caffeine.newBuilder()
                .maximumSize(LOCAL_DETAIL_MAXIMUM_SIZE)
                .expireAfterWrite(LOCAL_DETAIL_TTL)
                .build();
    }

    /**
     * 查询两级缓存；hit=true 且 detail=null 表示命中了 Redis 空值缓存。
     *
     * <p>一级缓存只会保存非空详情，所以不会让“暂时不存在”的结果在本机停留；
     * 这样商品上架后的可见性仍主要由 Redis 空值键和统一失效逻辑控制。</p>
     */
    public Lookup lookup(Long productId) {
        ProductDetailVO localDetail = localDetailCache.getIfPresent(productId);
        if (localDetail != null) {
            return new Lookup(true, localDetail);
        }

        String cachedValue = redisTemplate.opsForValue().get(key(productId));
        if (cachedValue == null) {
            return new Lookup(false, null);
        }
        if (NULL_VALUE.equals(cachedValue)) {
            return new Lookup(true, null);
        }

        try {
            ProductDetailVO redisDetail = objectMapper.readValue(cachedValue, ProductDetailVO.class);
            // Redis 命中后回填本机缓存，后续同一实例的热点读取无需再走网络。
            localDetailCache.put(productId, redisDetail);
            return new Lookup(true, redisDetail);
        } catch (JsonProcessingException exception) {
            // 旧版本或损坏的缓存直接删除，下次请求重新从数据库加载。
            localDetailCache.invalidate(productId);
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
            // 二级缓存写入成功后再回填一级缓存，避免本机单独保留一次序列化失败的数据。
            localDetailCache.put(detail.id(), detail);
        } catch (JsonProcessingException exception) {
            // 缓存失败不影响商品详情的正常返回。
        }
    }

    /** 短暂缓存不存在或不可公开的商品，减少无效请求反复查询数据库。 */
    public void putNull(Long productId) {
        // 同一个商品此前可能在本机缓存过正常详情；写空值前必须先清掉它。
        localDetailCache.invalidate(productId);
        redisTemplate.opsForValue().set(key(productId), NULL_VALUE, NULL_TTL);
    }

    /**
     * 商品数据变化后同时删除两级缓存。
     *
     * <p>该方法由各个 Service 的 afterCommit 回调调用：先清本机缓存可以让当前实例立即读取新数据，
     * 再删 Redis 让其他实例在其本机 TTL 到期或下次 Redis 查询时重新加载。</p>
     */
    public void invalidate(Long productId) {
        localDetailCache.invalidate(productId);
        redisTemplate.delete(key(productId));
    }

    /**
     * 读取商品详情缓存；未命中时只允许一个请求回源并重建缓存。
     *
     * <p>锁按商品 ID 划分，因此不同商品不会互相阻塞。获得锁后需要再次读取缓存：等待锁的
     * 这段时间内，持锁请求可能已经完成了回填。若 200ms 内仍未获得锁，宁可降级为一次直接
     * 查询，也不让请求无限等待；这只会发生在回源异常缓慢时，正常热点场景不会同时压到 MySQL。</p>
     *
     * @param productId 商品 ID
     * @param databaseLoader 缓存未命中时的数据库回源逻辑；可返回 null 表示商品不存在或不可见
     * @return 商品详情；null 表示不存在或不可见
     */
    public ProductDetailVO getOrLoad(Long productId, Supplier<ProductDetailVO> databaseLoader) {
        Lookup firstLookup = lookup(productId);
        if (firstLookup.hit()) {
            return firstLookup.detail();
        }

        RLock rebuildLock = redissonClient.getLock(rebuildLockKey(productId));
        boolean locked = false;
        try {
            locked = rebuildLock.tryLock(
                    REBUILD_LOCK_WAIT_MILLIS,
                    REBUILD_LOCK_LEASE_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            if (!locked) {
                // 锁持有时间异常过长时优先保障接口可用，避免请求线程持续堆积。
                return databaseLoader.get();
            }

            Lookup secondLookup = lookup(productId);
            if (secondLookup.hit()) {
                return secondLookup.detail();
            }

            ProductDetailVO loaded = databaseLoader.get();
            if (loaded == null) {
                putNull(productId);
            } else {
                putDetail(loaded);
            }
            return loaded;
        } catch (InterruptedException exception) {
            // 恢复中断标记，并降级查询；不能因为缓存锁被中断而把正常业务请求变成 500。
            Thread.currentThread().interrupt();
            return databaseLoader.get();
        } finally {
            // 租约过期后锁可能已被其他请求获得，只有当前线程仍持锁时才能释放。
            if (locked && rebuildLock.isHeldByCurrentThread()) {
                rebuildLock.unlock();
            }
        }
    }

    private String key(Long productId) {
        return KEY_PREFIX + productId;
    }

    private String rebuildLockKey(Long productId) {
        return REBUILD_LOCK_KEY_PREFIX + productId;
    }

    public record Lookup(boolean hit, ProductDetailVO detail) {
    }
}
