package com.campus.trade.product.integration;

import com.campus.trade.auth.dto.RegisterRequest;
import com.campus.trade.auth.jwt.JwtProvider;
import com.campus.trade.auth.service.AuthService;
import com.campus.trade.auth.service.LoginSessionService;
import com.campus.trade.category.entity.Category;
import com.campus.trade.category.mapper.CategoryMapper;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.product.dto.CreateProductRequest;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.vo.MyProductVO;
import com.campus.trade.product.vo.ProductDetailVO;
import com.campus.trade.product.vo.ProductIdVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品核心闭环的真实数据库集成测试。
 *
 * <p>该测试仅在 Maven 带 {@code -DrunExternalTests=true} 时运行，
 * 因此日常 {@code mvn test} 不会连接 Docker 中的 MySQL。
 * {@code @Transactional} 会在测试结束后整体回滚用户、商品、图片和审核日志，
 * 不会给开发库保留测试数据。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "runExternalTests", matches = "true")
class ProductFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private LoginSessionService loginSessionService;

    @Test
    void shouldCreateSubmitReviewAndExposeProductAfterApproval() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String studentNo = "P" + unique;
        String phone = "199" + String.format("%08d", Math.floorMod(unique.hashCode(), 100_000_000));
        Category category = categoryMapper.selectEnabledCategories().get(0);

        // 先走真实注册逻辑，获得已初始化信用摘要的卖家；测试事务结束后该用户会回滚。
        authService.register(new RegisterRequest(
                studentNo,
                phone,
                "Test1234",
                "商品集成测试用户",
                "东校区"
        ));

        // 注册接口不返回 userId，因此测试通过正常登录取得 JWT，再解析出后端实际使用的用户 ID。
        var login = authService.login(new com.campus.trade.auth.dto.LoginRequest(studentNo, "Test1234"));
        // 登录会写 Redis；finally 中删除当前会话，避免测试 token 遗留。
        var claims = jwtProvider.parse(login.token());

        try {
            UserContext.set(new CurrentUser(claims.userId(), 0, claims.tokenId()));
            ProductIdVO created = productService.create(new CreateProductRequest(
                    "集成测试商品",
                    "用于验证商品主表、图片表、审核日志与公开查询的映射。",
                    new BigDecimal("99.00"),
                    1,
                    1,
                    category.getId(),
                    "东校区",
                    "三食堂门口",
                    List.of("/uploads/integration-product.jpg")
            ));

            MyProductVO mine = productService.listMine(null).list().get(0);
            assertThat(mine.id()).isEqualTo(created.id());
            assertThat(mine.images()).containsExactly("/uploads/integration-product.jpg");
            assertThat(mine.status()).isZero();

            productService.submitReview(created.id());

            // 真实接口由 AdminInterceptor 保证角色；这里把同一用户上下文切为管理员以验证审核 SQL 与事务。
            UserContext.set(new CurrentUser(claims.userId(), 1, claims.tokenId()));
            productService.reviewByAdmin(created.id(), true, null);

            ProductDetailVO detail = productService.getPublicDetail(created.id());
            assertThat(detail.status()).isEqualTo(3);
            assertThat(detail.categoryName()).isEqualTo(category.getName());
            assertThat(detail.images()).containsExactly("/uploads/integration-product.jpg");
            assertThat(detail.seller().nickname()).isEqualTo("商品集成测试用户");
            assertThat(productService.listPublic(null, null, null, null, null, 1, 10).list())
                    .extracting(item -> item.id())
                    .contains(created.id());
        } finally {
            UserContext.clear();
            // AuthService.login 创建的 Redis 白名单不受数据库事务管理，必须显式删除。
            loginSessionService.delete(claims.tokenId());
        }
    }
}
