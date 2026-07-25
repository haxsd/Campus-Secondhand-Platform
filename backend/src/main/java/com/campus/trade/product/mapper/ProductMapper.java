package com.campus.trade.product.mapper;

import com.campus.trade.product.entity.Product;
import com.campus.trade.product.model.ProductSearchCriteria;
import com.campus.trade.product.model.ReviewSummary;
import com.campus.trade.product.model.SellerSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 商品模块的数据访问接口。
 *
 * <p>SQL 全部位于 resources/mapper/product/ProductMapper.xml。
 * 本接口只做数据读写，不判断“当前用户是否是卖家”或“状态能否流转”等业务规则，
 * 这些规则由 ProductService 处理。</p>
 */
@Mapper
public interface ProductMapper {

    /** 插入商品主记录，并把数据库自增主键回填到 product.id。 */
    int insert(Product product);

    /** 删除某商品现有的图片关系，用于编辑时整体替换图片顺序。 */
    int deleteImagesByProductId(@Param("productId") Long productId);

    /** 批量插入商品图片，列表顺序就是图片展示顺序。 */
    int insertImages(@Param("productId") Long productId, @Param("urls") List<String> urls);

    /** 按主键查询未软删除商品，不限制商品状态，供卖家和管理员操作前确认状态。 */
    Optional<Product> selectById(@Param("id") Long id);

    /** 查询公开可见的在售商品列表。 */
    List<Product> selectPublicProducts(ProductSearchCriteria criteria);

    /** 查询公开商品列表总数，必须与 selectPublicProducts 使用相同过滤条件。 */
    long countPublicProducts(ProductSearchCriteria criteria);

    /** 查询公开可见的单件商品；草稿、待审核和已下架商品不会被查出。 */
    Optional<Product> selectPublicById(@Param("id") Long id);

    /** 查询指定卖家的全部未删除商品，可按状态筛选。 */
    List<Product> selectBySeller(@Param("sellerId") Long sellerId, @Param("status") Integer status);

    /** 查询所有待审核商品，供管理员审核列表使用。 */
    List<Product> selectPendingProducts();

    /** 查询商品图片地址，按 sort 和主键稳定排序。 */
    List<String> selectImageUrlsByProductId(@Param("productId") Long productId);

    /** 查询商品第一张图片，列表用作封面；没有图片时返回 null。 */
    String selectCoverByProductId(@Param("productId") Long productId);

    /** 查询分类名称；分类已删除或不存在时返回 null。 */
    String selectCategoryNameById(@Param("categoryId") Long categoryId);

    /** 查询卖家公开信息及信用摘要。 */
    Optional<SellerSummary> selectSellerSummary(@Param("sellerId") Long sellerId);

    /** 查询卖家最近的可见评价，最多由 SQL 返回 5 条。 */
    List<ReviewSummary> selectRecentReviewsBySellerId(@Param("sellerId") Long sellerId);

    /** 查询商品最近一次驳回审核的原因，非驳回状态时 Service 不会使用该字段。 */
    String selectLatestRejectReason(@Param("productId") Long productId);

    /**
     * 卖家编辑商品时更新内容并校验乐观锁版本。
     *
     * <p>SQL 内部限制商品必须属于卖家且状态为草稿、驳回或已下架。</p>
     */
    int updateContentBySellerAndVersion(Product product);

    /** 卖家将草稿、驳回或已下架商品提交为待审核。 */
    int submitReviewBySeller(@Param("id") Long id, @Param("sellerId") Long sellerId);

    /** 卖家撤回待审核商品，回到草稿。 */
    int withdrawReviewBySeller(@Param("id") Long id, @Param("sellerId") Long sellerId);

    /** 卖家下架在售商品。 */
    int offShelfBySeller(@Param("id") Long id, @Param("sellerId") Long sellerId);

    /**
     * 在售商品调整库存；SQL 会同时校验归属、状态及减少后的库存下限。
     */
    int adjustStockBySeller(
            @Param("id") Long id,
            @Param("sellerId") Long sellerId,
            @Param("delta") Integer delta
    );

    /**
     * 买家创建订单时原子扣减库存。
     *
     * <p>库存判断必须写进同一条 UPDATE，不能采用“先查库存、再扣库存”，
     * 否则两个买家同时下单时可能超卖。扣减后库存为 0 时，商品转为售罄状态。</p>
     */
    int decreaseStockForOrder(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 订单取消时归还此前占用的库存。
     *
     * <p>若商品因为库存扣至 0 而变为售罄，库存恢复后同时重新标记为在售；
     * 已软删除的商品不恢复，避免取消订单把已删除商品重新展示。</p>
     */
    int restoreStockForCancelledOrder(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 管理员审核时按“仍处于待审核”这一前置状态更新商品。 */
    int reviewByAdmin(@Param("id") Long id, @Param("targetStatus") Integer targetStatus);

    /** 写入管理员审核日志，保留驳回原因和审核人。 */
    int insertReviewLog(
            @Param("productId") Long productId,
            @Param("reviewerId") Long reviewerId,
            @Param("result") Integer result,
            @Param("reason") String reason
    );
}
