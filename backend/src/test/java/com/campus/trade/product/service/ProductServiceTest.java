package com.campus.trade.product.service;

import com.campus.trade.category.mapper.CategoryMapper;
import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.history.service.BrowseHistoryService;
import com.campus.trade.product.dto.CreateProductRequest;
import com.campus.trade.product.dto.UpdateProductRequest;
import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.model.ProductStatus;
import com.campus.trade.product.vo.ProductIdVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品核心业务的纯单元测试。
 *
 * <p>Mapper 和分类查询都由 Mockito 模拟，因此测试不连接 MySQL 或 Redis。
 * 测试目标是验证 Service 是否正确执行权限、状态机、事务内操作顺序和并发冲突处理。</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductDetailCacheService productDetailCacheService;

    @Mock
    private BrowseHistoryService browseHistoryService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productMapper,
                categoryMapper,
                productDetailCacheService,
                browseHistoryService
        );
    }

    @AfterEach
    void tearDown() {
        // 生产环境由 LoginInterceptor.afterCompletion 清理；单元测试必须手动清理 ThreadLocal。
        UserContext.clear();
    }

    @Test
    void shouldCreateDraftWithCurrentUserAsSellerAndSaveImages() {
        // 模拟登录拦截器已经认证用户；请求 DTO 中没有 sellerId，因此归属只能来自这里。
        UserContext.set(new CurrentUser(7L, 0, "token-7"));
        when(categoryMapper.existsEnabledById(1L)).thenReturn(true);
        doAnswer(invocation -> {
            Product inserted = invocation.getArgument(0);
            // 模拟 MyBatis 的 useGeneratedKeys 回填数据库自增主键。
            inserted.setId(101L);
            return 1;
        }).when(productMapper).insert(any(Product.class));

        ProductIdVO result = productService.create(createRequest());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product inserted = productCaptor.getValue();
        assertThat(result.id()).isEqualTo(101L);
        assertThat(inserted.getSellerId()).isEqualTo(7L);
        assertThat(inserted.getStatus()).isEqualTo(ProductStatus.DRAFT.getCode());
        assertThat(inserted.getTitle()).isEqualTo("九成新键盘");
        verify(productMapper).insertImages(101L, List.of("/uploads/keyboard-1.jpg"));
    }

    @Test
    void shouldRejectUpdateWhenOptimisticLockVersionDoesNotMatch() {
        UserContext.set(new CurrentUser(7L, 0, "token-7"));
        Product existing = product(7L, ProductStatus.DRAFT.getCode());
        existing.setVersion(5);
        when(categoryMapper.existsEnabledById(1L)).thenReturn(true);
        when(productMapper.selectById(101L)).thenReturn(Optional.of(existing));
        // 数据库更新影响 0 行，代表另一个请求已经改变 version 或状态。
        when(productMapper.updateContentBySellerAndVersion(any(Product.class))).thenReturn(0);

        assertThatThrownBy(() -> productService.update(101L, updateRequest()))
                .isInstanceOf(BizException.class)
                .hasMessage("商品已被其他请求修改，请刷新后重试")
                .extracting(exception -> ((BizException) exception).getCode())
                .isEqualTo(409);

        // 乐观锁失败时不能删旧图，否则会破坏另一位编辑者已经保存的图片。
        verify(productMapper, never()).deleteImagesByProductId(101L);
    }

    @Test
    void shouldRejectSellerOperationOnAnotherUsersProduct() {
        UserContext.set(new CurrentUser(7L, 0, "token-7"));
        when(productMapper.selectById(101L))
                .thenReturn(Optional.of(product(8L, ProductStatus.ON_SALE.getCode())));

        assertThatThrownBy(() -> productService.offShelf(101L))
                .isInstanceOf(BizException.class)
                .hasMessage("无权操作该商品")
                .extracting(exception -> ((BizException) exception).getCode())
                .isEqualTo(403);

        verify(productMapper, never()).offShelfBySeller(any(), any());
    }

    @Test
    void shouldReviewPendingProductAndWriteAuditLog() {
        // 管理员角色通常由 AdminInterceptor 校验；Service 仍从 UserContext 获取审核人 ID 写日志。
        UserContext.set(new CurrentUser(99L, 1, "admin-token"));
        when(productMapper.selectById(101L))
                .thenReturn(Optional.of(product(7L, ProductStatus.PENDING_REVIEW.getCode())));
        when(productMapper.reviewByAdmin(101L, ProductStatus.ON_SALE.getCode())).thenReturn(1);

        productService.reviewByAdmin(101L, true, null);

        verify(productMapper).reviewByAdmin(101L, ProductStatus.ON_SALE.getCode());
        verify(productMapper).insertReviewLog(101L, 99L, 1, null);
    }

    private CreateProductRequest createRequest() {
        return new CreateProductRequest(
                " 九成新键盘 ",
                " 使用正常，带数据线 ",
                new BigDecimal("88.00"),
                1,
                1,
                1L,
                " 东校区 ",
                " 三食堂门口 ",
                List.of(" /uploads/keyboard-1.jpg ")
        );
    }

    private UpdateProductRequest updateRequest() {
        return new UpdateProductRequest(
                "九成新键盘",
                "使用正常，带数据线",
                new BigDecimal("88.00"),
                1,
                1,
                1L,
                "东校区",
                "三食堂门口",
                List.of("/uploads/keyboard-1.jpg"),
                // 本测试固定模拟前端拿到的旧版本号 4，用于触发乐观锁冲突。
                4
        );
    }

    private Product product(Long sellerId, int status) {
        Product product = new Product();
        // 本测试所有操作围绕同一个商品 ID，避免把无关变量传入辅助方法。
        product.setId(101L);
        product.setSellerId(sellerId);
        product.setCategoryId(1L);
        product.setStatus(status);
        product.setStock(1);
        product.setVersion(0);
        return product;
    }
}
