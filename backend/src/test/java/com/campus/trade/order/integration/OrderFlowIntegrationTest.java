package com.campus.trade.order.integration;

import com.campus.trade.auth.dto.LoginRequest;
import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.service.LoginSessionService;
import com.campus.trade.category.entity.Category;
import com.campus.trade.category.mapper.CategoryMapper;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.order.dto.CreateOrderRequest;
import com.campus.trade.order.service.OrderService;
import com.campus.trade.order.vo.OrderCreatedVO;
import com.campus.trade.order.vo.OrderDetailVO;
import com.campus.trade.product.dto.CreateProductRequest;
import com.campus.trade.product.service.ProductDetailCacheService;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.vo.ProductIdVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单最小闭环的真实 MySQL/Redis 集成测试。
 *
 * <p>只在显式传入 -DrunExternalTests=true 时执行。测试事务结束后回滚用户、商品、订单、
 * 快照和状态日志；两个登录 token 与 Redis 缓存键则在 finally 中主动清理。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "runExternalTests", matches = "true")
class OrderFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private LoginSessionService loginSessionService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductDetailCacheService productDetailCacheService;

    @Test
    void shouldCreateIdempotentlyConfirmAndCompleteOrder() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String sellerStudentNo = "S" + unique;
        String buyerStudentNo = "B" + unique;
        String sellerPhone = "188" + String.format("%08d", Math.floorMod(unique.hashCode(), 100_000_000));
        String buyerPhone = "177" + String.format("%08d", Math.floorMod(unique.hashCode() + 1, 100_000_000));
        Category category = categoryMapper.selectEnabledCategories().get(0);

        // 创建真实卖家和买家，再通过正常登录流程取得数据库实际分配的用户 ID。
        authService.register(new RegisterRequest(sellerStudentNo, sellerPhone, "Test1234", "订单测试卖家", "东校区"));
        authService.register(new RegisterRequest(buyerStudentNo, buyerPhone, "Test1234", "订单测试买家", "东校区"));
        var sellerClaims = jwtProvider.parse(authService.login(new LoginRequest(sellerStudentNo, "Test1234")).token());
        var buyerClaims = jwtProvider.parse(authService.login(new LoginRequest(buyerStudentNo, "Test1234")).token());

        Long productId = null;
        try {
            // 卖家创建并审核一件库存为 2 的商品，使其满足真实下单条件。
            UserContext.set(new CurrentUser(sellerClaims.userId(), 0, sellerClaims.tokenId()));
            ProductIdVO product = productService.create(new CreateProductRequest(
                    "订单集成测试商品",
                    "用于验证订单、快照、库存和状态日志的真实数据库闭环。",
                    new BigDecimal("66.00"),
                    2,
                    1,
                    category.getId(),
                    "东校区",
                    "图书馆门口",
                    List.of("/api/uploads/order-integration.jpg")
            ));
            productId = product.id();
            productService.submitReview(productId);
            // 集成测试直接调用 Service，因此临时使用管理员角色验证审核后的真实订单条件。
            UserContext.set(new CurrentUser(sellerClaims.userId(), 1, sellerClaims.tokenId()));
            productService.reviewByAdmin(productId, true, null);

            // 买家下单两件；同一个 requestId 重试必须返回同一订单，而不是再次扣库存。
            UserContext.set(new CurrentUser(buyerClaims.userId(), 0, buyerClaims.tokenId()));
            CreateOrderRequest request = new CreateOrderRequest(
                    productId,
                    2,
                    LocalDateTime.of(2026, 7, 26, 18, 0),
                    "图书馆门口",
                    "请提前联系",
                    "order-request-" + unique
            );
            OrderCreatedVO created = orderService.create(request);
            OrderCreatedVO retried = orderService.create(request);
            assertThat(retried.id()).isEqualTo(created.id());
            assertThat(created.status()).isZero();

            OrderDetailVO pendingDetail = orderService.getDetail(created.id());
            assertThat(pendingDetail.snapshot().title()).isEqualTo("订单集成测试商品");
            assertThat(pendingDetail.snapshot().images()).containsExactly("/api/uploads/order-integration.jpg");
            assertThat(pendingDetail.canConfirm()).isFalse();
            assertThat(pendingDetail.canComplete()).isFalse();

            // 卖家确认后，买家可见“确认完成”操作；最后写入一条完成状态日志。
            UserContext.set(new CurrentUser(sellerClaims.userId(), 0, sellerClaims.tokenId()));
            orderService.confirm(created.id());
            UserContext.set(new CurrentUser(buyerClaims.userId(), 0, buyerClaims.tokenId()));
            orderService.complete(created.id());
            OrderDetailVO completedDetail = orderService.getDetail(created.id());
            assertThat(completedDetail.status()).isEqualTo(2);
            assertThat(completedDetail.logs()).hasSize(2);
            assertThat(completedDetail.logs().get(0).toStatus()).isEqualTo(1);
            assertThat(completedDetail.logs().get(1).toStatus()).isEqualTo(2);
        } finally {
            UserContext.clear();
            loginSessionService.delete(sellerClaims.tokenId());
            loginSessionService.delete(buyerClaims.tokenId());
            if (productId != null) {
                productDetailCacheService.invalidate(productId);
            }
        }
    }
}
