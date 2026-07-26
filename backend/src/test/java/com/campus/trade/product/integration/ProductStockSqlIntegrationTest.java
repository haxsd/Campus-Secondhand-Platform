package com.campus.trade.product.integration;

import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.model.ProductStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 库存相关条件 UPDATE 的真实 SQL 行为测试。
 *
 * <p>为什么必须连真实 MySQL：这几条 SQL 的正确性取决于数据库自身的求值规则。
 * MySQL 的 {@code UPDATE ... SET a = ..., b = ...} 是<b>从左到右</b>依次求值并赋值的，
 * 后面的表达式读到的是前面刚写入的新值。历史上 {@code decreaseStockForOrder} 就是因为把
 * {@code status = CASE ...} 写在 {@code stock = stock - n} 之后，导致 CASE 里的库存被重复扣了一次，
 * 出现“卖光了还挂在售”“还有货却显示售罄”。
 *
 * <p>用 Mockito 模拟 Mapper 的单元测试只能断言“调用返回 1”，永远发现不了这类问题，
 * 因此这里直接把 SQL 打到数据库上验证。测试整体在事务里执行，结束后回滚，不留脏数据。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "runExternalTests", matches = "true")
class ProductStockSqlIntegrationTest {

    /** 与真实用户无关的测试卖家 ID；表之间没有物理外键，可以直接使用。 */
    private static final long TEST_SELLER_ID = 900_000_001L;

    @Autowired
    private ProductMapper productMapper;

    @Test
    void shouldMarkSoldOutOnlyWhenLastItemIsTakenAway() {
        Long productId = insertOnSaleProduct(2);

        // 两件卖掉一件：仍然有货，必须保持在售，否则商品会凭空从首页消失。
        assertThat(productMapper.decreaseStockForOrder(productId, 1)).isEqualTo(1);
        Product afterFirst = reload(productId);
        assertThat(afterFirst.getStock()).isEqualTo(1);
        assertThat(afterFirst.getStatus()).isEqualTo(ProductStatus.ON_SALE.getCode());

        // 卖掉最后一件：库存归零，同一条 SQL 必须把商品置为售罄。
        assertThat(productMapper.decreaseStockForOrder(productId, 1)).isEqualTo(1);
        Product afterSecond = reload(productId);
        assertThat(afterSecond.getStock()).isZero();
        assertThat(afterSecond.getStatus()).isEqualTo(ProductStatus.SOLD_OUT.getCode());

        // 售罄后条件不再满足，重复扣减影响 0 行，不会出现负库存。
        assertThat(productMapper.decreaseStockForOrder(productId, 1)).isZero();
    }

    @Test
    void shouldRejectDecreaseWhenStockIsNotEnough() {
        Long productId = insertOnSaleProduct(1);

        assertThat(productMapper.decreaseStockForOrder(productId, 2)).isZero();
        assertThat(reload(productId).getStock()).isEqualTo(1);
    }

    @Test
    void shouldRestoreOnSaleWhenSellerRestocksSoldOutProduct() {
        Long productId = insertOnSaleProduct(1);
        productMapper.decreaseStockForOrder(productId, 1);
        assertThat(reload(productId).getStatus()).isEqualTo(ProductStatus.SOLD_OUT.getCode());

        // 售罄不是死状态：卖家补货后商品自动回到在售。
        assertThat(productMapper.adjustStockBySeller(productId, TEST_SELLER_ID, 3)).isEqualTo(1);
        Product restocked = reload(productId);
        assertThat(restocked.getStock()).isEqualTo(3);
        assertThat(restocked.getStatus()).isEqualTo(ProductStatus.ON_SALE.getCode());
    }

    @Test
    void shouldAllowSellerToOffShelfSoldOutProduct() {
        Long productId = insertOnSaleProduct(1);
        productMapper.decreaseStockForOrder(productId, 1);

        // 售罄商品也能下架，下架后回到 4，卖家才可以重新编辑。
        assertThat(productMapper.offShelfBySeller(productId, TEST_SELLER_ID)).isEqualTo(1);
        assertThat(reload(productId).getStatus()).isEqualTo(ProductStatus.OFF_SHELF.getCode());
    }

    @Test
    void shouldRestoreStockAndOnSaleWhenOrderIsCancelled() {
        Long productId = insertOnSaleProduct(1);
        productMapper.decreaseStockForOrder(productId, 1);

        assertThat(productMapper.restoreStockForCancelledOrder(productId, 1)).isEqualTo(1);
        Product restored = reload(productId);
        assertThat(restored.getStock()).isEqualTo(1);
        assertThat(restored.getStatus()).isEqualTo(ProductStatus.ON_SALE.getCode());
    }

    private Long insertOnSaleProduct(int stock) {
        Product product = new Product();
        product.setSellerId(TEST_SELLER_ID);
        product.setCategoryId(1L);
        product.setTitle("库存 SQL 测试商品");
        product.setDescription("仅用于验证条件更新的真实 SQL 行为，测试结束后随事务回滚。");
        product.setPrice(new BigDecimal("12.34"));
        product.setStock(stock);
        product.setItemCondition(1);
        product.setCampus("东校区");
        product.setTradePlace("图书馆门口");
        // 正常链路里状态由 ProductService 设置，这里直接调 Mapper，需要自己给出初始草稿状态。
        product.setStatus(ProductStatus.DRAFT.getCode());
        // insert 写入的是草稿(0)，走一遍“提交审核 → 管理员通过”才能到达在售(3)。
        productMapper.insert(product);
        productMapper.submitReviewBySeller(product.getId(), TEST_SELLER_ID);
        productMapper.reviewByAdmin(product.getId(), ProductStatus.ON_SALE.getCode());
        return product.getId();
    }

    private Product reload(Long productId) {
        return productMapper.selectById(productId).orElseThrow();
    }
}
