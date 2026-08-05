package com.campus.trade.product.service;

import com.campus.trade.product.vo.ProductDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 商品详情缓存重建的单元测试。
 *
 * <p>测试只模拟 Redis 与 Redisson，不需要启动外部服务；重点验证缓存未命中时只有持锁请求
 * 执行回源并写回缓存。</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductDetailCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rebuildLock;

    @Test
    void shouldRebuildMissedDetailCacheWhileHoldingProductScopedLock() throws InterruptedException {
        long productId = 101L;
        ProductDetailCacheService cacheService = new ProductDetailCacheService(
                redisTemplate,
                new ObjectMapper(),
                redissonClient
        );
        ProductDetailVO expected = detail(productId);
        AtomicInteger loaderCalls = new AtomicInteger();

        // 首次读取和获得锁后的二次读取都未命中，当前请求才需要真正回源。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product:detail:" + productId)).thenReturn(null);
        when(redissonClient.getLock("lock:product:detail:" + productId)).thenReturn(rebuildLock);
        when(rebuildLock.tryLock(anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(rebuildLock.isHeldByCurrentThread()).thenReturn(true);

        ProductDetailVO actual = cacheService.getOrLoad(productId, () -> {
            loaderCalls.incrementAndGet();
            return expected;
        });

        assertThat(actual).isSameAs(expected);
        assertThat(loaderCalls).hasValue(1);
        // 正常详情重建完成后应写入带 TTL 的详情缓存，而不是只返回数据库结果。
        verify(valueOperations).set(
                eq("product:detail:" + productId),
                anyString(),
                any(Duration.class)
        );
        verify(rebuildLock).unlock();
        // 锁名称含商品 ID，证明不同商品的缓存重建不会互相阻塞。
        verify(redissonClient).getLock("lock:product:detail:" + productId);
    }

    @Test
    void shouldServeDetailFromCaffeineWithoutReadingRedisAgain() {
        long productId = 102L;
        ProductDetailCacheService cacheService = new ProductDetailCacheService(
                redisTemplate,
                new ObjectMapper(),
                redissonClient
        );
        ProductDetailVO expected = detail(productId);

        // 首次写入会写 Redis 并同步回填 Caffeine；清空 Mock 调用记录后再查询，
        // 可以精确验证第二次读取没有访问 Redis 或 Redisson。
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService.putDetail(expected);
        org.mockito.Mockito.clearInvocations(redisTemplate, valueOperations, redissonClient);

        ProductDetailCacheService.Lookup lookup = cacheService.lookup(productId);

        assertThat(lookup.hit()).isTrue();
        assertThat(lookup.detail()).isSameAs(expected);
        verifyNoInteractions(redisTemplate, valueOperations, redissonClient);
    }

    private ProductDetailVO detail(long productId) {
        return new ProductDetailVO(
                productId,
                "测试商品",
                "用于验证缓存重建锁",
                new BigDecimal("10.00"),
                1,
                1,
                "东校区",
                "图书馆",
                3,
                1L,
                "数码",
                0,
                List.of(),
                null,
                List.of()
        );
    }
}
