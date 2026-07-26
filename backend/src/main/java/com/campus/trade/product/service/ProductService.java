package com.campus.trade.product.service;

import com.campus.trade.category.mapper.CategoryMapper;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.PageResult;
import com.campus.trade.history.service.BrowseHistoryService;
import com.campus.trade.product.dto.CreateProductRequest;
import com.campus.trade.product.dto.StockAdjustRequest;
import com.campus.trade.product.dto.UpdateProductRequest;
import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.model.ItemCondition;
import com.campus.trade.product.model.ProductSearchCriteria;
import com.campus.trade.product.model.ProductStatus;
import com.campus.trade.product.model.ReviewSummary;
import com.campus.trade.product.model.SellerSummary;
import com.campus.trade.product.vo.MyProductVO;
import com.campus.trade.product.vo.PendingProductVO;
import com.campus.trade.product.vo.ProductDetailVO;
import com.campus.trade.product.vo.ProductIdVO;
import com.campus.trade.product.vo.ProductListVO;
import com.campus.trade.product.vo.ProductSellerVO;
import com.campus.trade.product.vo.RecentReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 商品发布、卖家管理、公开查询与管理员审核的业务服务。
 *
 * <p>商品状态机规则集中在本类，不放在 Controller 或前端：
 * <ul>
 *     <li>草稿、驳回、已下架：可以编辑或申请审核；</li>
 *     <li>待审核：只能撤回审核申请；</li>
 *     <li>在售：可以下架或调整库存；</li>
 *     <li>管理员只能审核待审核商品。</li>
 * </ul>
 * SQL 也使用状态条件作为最后一道并发保护，避免两个请求同时通过 Java 判断后产生非法状态。</p>
 */
@Service
public class ProductService {

    private static final Set<Integer> EDITABLE_STATUSES = Set.of(
            ProductStatus.DRAFT.getCode(),
            ProductStatus.REJECTED.getCode(),
            ProductStatus.OFF_SHELF.getCode()
    );

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductDetailCacheService productDetailCacheService;
    private final BrowseHistoryService browseHistoryService;

    public ProductService(
            ProductMapper productMapper,
            CategoryMapper categoryMapper,
            ProductDetailCacheService productDetailCacheService,
            BrowseHistoryService browseHistoryService
    ) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.productDetailCacheService = productDetailCacheService;
        this.browseHistoryService = browseHistoryService;
    }

    /**
     * 创建一件状态为“草稿”的商品，并保存图片列表。
     */
    @Transactional
    public ProductIdVO create(CreateProductRequest request) {
        validateProductForm(request.itemCondition(), request.categoryId());

        Product product = new Product();
        product.setSellerId(UserContext.requireCurrentUser().userId());
        product.setCategoryId(request.categoryId());
        product.setTitle(request.title().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setItemCondition(request.itemCondition());
        product.setCampus(request.campus().trim());
        product.setTradePlace(request.tradePlace().trim());

        // 新商品必须先以草稿创建，不能由请求直接伪造为“在售”。
        product.setStatus(ProductStatus.DRAFT.getCode());
        productMapper.insert(product);

        // 主键由 MyBatis 回填后，再将每张图片关联到这件商品。
        productMapper.insertImages(product.getId(), normalizeImageUrls(request.images()));
        return new ProductIdVO(product.getId());
    }

    /**
     * 编辑本人处于可编辑状态的商品，并通过 version 防止覆盖他人的最新修改。
     */
    @Transactional
    public void update(Long productId, UpdateProductRequest request) {
        validateProductForm(request.itemCondition(), request.categoryId());
        Long sellerId = UserContext.requireCurrentUser().userId();
        Product existing = requireOwnedProduct(productId, sellerId);
        requireEditable(existing);

        Product product = new Product();
        product.setId(productId);
        product.setSellerId(sellerId);
        product.setCategoryId(request.categoryId());
        product.setTitle(request.title().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setItemCondition(request.itemCondition());
        product.setCampus(request.campus().trim());
        product.setTradePlace(request.tradePlace().trim());
        product.setVersion(request.version());

        // SQL 同时判断 seller、status 与 version；影响行数为 0 表示数据已经被其他请求改变。
        if (productMapper.updateContentBySellerAndVersion(product) == 0) {
            throw conflict("商品已被其他请求修改，请刷新后重试");
        }

        // 图片没有独立编辑接口，因此采用“删旧图 + 写新图”的事务性整体替换。
        productMapper.deleteImagesByProductId(productId);
        productMapper.insertImages(productId, normalizeImageUrls(request.images()));
        invalidateDetailCacheAfterCommit(productId);
    }

    /**
     * 卖家将草稿、驳回或已下架商品提交为待审核状态。
     */
    @Transactional
    public void submitReview(Long productId) {
        Long sellerId = UserContext.requireCurrentUser().userId();
        Product product = requireOwnedProduct(productId, sellerId);
        requireStatus(product, EDITABLE_STATUSES, "当前状态不能申请上架");
        if (product.getStock() <= 0) {
            throw conflict("库存为 0，无法申请上架");
        }

        if (productMapper.submitReviewBySeller(productId, sellerId) == 0) {
            throw conflict("商品状态已变化，请刷新后重试");
        }
        invalidateDetailCacheAfterCommit(productId);
    }

    /**
     * 卖家撤回待审核申请，商品重新回到草稿状态。
     */
    @Transactional
    public void withdrawReview(Long productId) {
        Long sellerId = UserContext.requireCurrentUser().userId();
        Product product = requireOwnedProduct(productId, sellerId);
        requireStatus(product, Set.of(ProductStatus.PENDING_REVIEW.getCode()), "只有待审核的商品能撤回申请");

        if (productMapper.withdrawReviewBySeller(productId, sellerId) == 0) {
            throw conflict("商品状态已变化，请刷新后重试");
        }
        invalidateDetailCacheAfterCommit(productId);
    }

    /**
     * 卖家下架自己已公开展示的商品。
     *
     * <p>在售(3) 与售罄(5) 都属于已公开展示，都可以下架；下架后商品回到可编辑的状态(4)。</p>
     */
    @Transactional
    public void offShelf(Long productId) {
        Long sellerId = UserContext.requireCurrentUser().userId();
        Product product = requireOwnedProduct(productId, sellerId);
        requireStatus(
                product,
                Set.of(ProductStatus.ON_SALE.getCode(), ProductStatus.SOLD_OUT.getCode()),
                "只有在售或已售罄的商品能下架"
        );

        if (productMapper.offShelfBySeller(productId, sellerId) == 0) {
            throw conflict("商品状态已变化，请刷新后重试");
        }
        invalidateDetailCacheAfterCommit(productId);
    }

    /**
     * 增减商品库存。
     *
     * <p>在售(3) 商品减少库存后至少保留一件，减到零由订单模块扣减库存时才会置为售罄；
     * 售罄(5) 商品补货后由 SQL 自动恢复为在售(3)，否则商品卖完一次就永远卖不出去了。</p>
     */
    @Transactional
    public void adjustStock(Long productId, StockAdjustRequest request) {
        if (request.delta() == 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "库存增减数量不能为 0");
        }

        Long sellerId = UserContext.requireCurrentUser().userId();
        Product product = requireOwnedProduct(productId, sellerId);
        requireStatus(
                product,
                Set.of(ProductStatus.ON_SALE.getCode(), ProductStatus.SOLD_OUT.getCode()),
                "只有在售或已售罄的商品能调整库存"
        );

        if (product.getStock() + request.delta() < 1) {
            throw conflict("库存减少后至少要保留 1 件");
        }

        if (productMapper.adjustStockBySeller(productId, sellerId, request.delta()) == 0) {
            throw conflict("商品库存已变化，请刷新后重试");
        }
        invalidateDetailCacheAfterCommit(productId);
    }

    /**
     * 查询公开商品列表。只有在售且库存大于零的商品会进入结果。
     */
    @Transactional(readOnly = true)
    public PageResult<ProductListVO> listPublic(
            String keyword,
            Long categoryId,
            String campus,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int pageSize
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "最低价格不能大于最高价格");
        }

        ProductSearchCriteria criteria = new ProductSearchCriteria(
                trimToNull(keyword),
                categoryId,
                trimToNull(campus),
                minPrice,
                maxPrice,
                (page - 1) * pageSize,
                pageSize
        );
        List<ProductListVO> list = productMapper.selectPublicProducts(criteria).stream()
                .map(product -> new ProductListVO(
                        product.getId(),
                        product.getTitle(),
                        product.getPrice(),
                        productMapper.selectCoverByProductId(product.getId()),
                        product.getCampus(),
                        product.getItemCondition(),
                        product.getStock(),
                        product.getCreatedAt()
                ))
                .toList();
        return new PageResult<>(list, productMapper.countPublicProducts(criteria), page, pageSize);
    }

    /**
     * 查询公开商品详情，并组合分类、卖家信用、图片和最近评价。
     */
    public ProductDetailVO getPublicDetail(Long productId) {
        ProductDetailCacheService.Lookup lookup = productDetailCacheService.lookup(productId);
        ProductDetailVO detail;
        if (lookup.hit()) {
            detail = lookup.detail();
        } else {
            detail = loadPublicDetail(productId);
            if (detail == null) {
                productDetailCacheService.putNull(productId);
            } else {
                productDetailCacheService.putDetail(detail);
            }
        }

        if (detail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在或已下架");
        }

        // 缓存命中时同样要记录浏览；记录行为与详情数据来自缓存还是 MySQL 无关。
        browseHistoryService.recordIfAuthenticated(productId);
        return detail;
    }

    /**
     * 从 MySQL 组装公开详情。返回 null 表示商品不存在或当前不可公开展示，交由调用方写空值缓存。
     */
    private ProductDetailVO loadPublicDetail(Long productId) {
        Product product = productMapper.selectPublicById(productId).orElse(null);
        if (product == null) {
            return null;
        }

        SellerSummary seller = productMapper.selectSellerSummary(product.getSellerId())
                .orElse(null);
        if (seller == null) {
            // 数据完整性异常时不暴露半成品商品详情，也不向 Redis 写入长期正常缓存。
            return null;
        }
        ProductSellerVO sellerVO = toSellerVO(seller);
        List<RecentReviewVO> reviews = productMapper.selectRecentReviewsBySellerId(product.getSellerId())
                .stream()
                .map(this::toRecentReviewVO)
                .toList();

        return new ProductDetailVO(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getItemCondition(),
                product.getCampus(),
                product.getTradePlace(),
                product.getStatus(),
                product.getCategoryId(),
                productMapper.selectCategoryNameById(product.getCategoryId()),
                product.getViewCount(),
                productMapper.selectImageUrlsByProductId(product.getId()),
                sellerVO,
                reviews
        );
    }

    /**
     * 查询当前卖家的商品，包含草稿、驳回等不公开状态，供“我的商品”页面管理。
     */
    @Transactional(readOnly = true)
    public PageResult<MyProductVO> listMine(Integer status) {
        if (status != null && !ProductStatus.isValid(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品状态不正确");
        }

        Long sellerId = UserContext.requireCurrentUser().userId();
        List<MyProductVO> list = productMapper.selectBySeller(sellerId, status).stream()
                .map(this::toMyProductVO)
                .toList();
        // 当前前端不分页“我的商品”，仍使用 PageResult 保持接口数据结构统一。
        return new PageResult<>(list, list.size(), 1, list.size());
    }

    /**
     * 查询管理员待审核商品列表。
     */
    @Transactional(readOnly = true)
    public PageResult<PendingProductVO> listPending() {
        List<PendingProductVO> list = productMapper.selectPendingProducts().stream()
                .map(this::toPendingProductVO)
                .toList();
        return new PageResult<>(list, list.size(), 1, list.size());
    }

    /**
     * 管理员审核前读取完整资料，不使用公开详情缓存。
     * 待审核商品不能被游客读取，审核写操作仍会再次校验状态防止旧页面误操作。
     */
    @Transactional(readOnly = true)
    public ProductDetailVO getPendingDetailForAdmin(Long productId) {
        Product product = productMapper.selectById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        requireStatus(product, Set.of(ProductStatus.PENDING_REVIEW.getCode()), "商品已不在待审核状态");
        SellerSummary seller = productMapper.selectSellerSummary(product.getSellerId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "卖家不存在"));
        return new ProductDetailVO(
                product.getId(), product.getTitle(), product.getDescription(), product.getPrice(), product.getStock(),
                product.getItemCondition(), product.getCampus(), product.getTradePlace(), product.getStatus(),
                product.getCategoryId(), productMapper.selectCategoryNameById(product.getCategoryId()), product.getViewCount(),
                productMapper.selectImageUrlsByProductId(product.getId()), toSellerVO(seller),
                productMapper.selectRecentReviewsBySellerId(product.getSellerId()).stream().map(this::toRecentReviewVO).toList()
        );
    }

    /**
     * 管理员审核待审核商品：通过后上架，驳回后记录原因。
     */
    @Transactional
    public void reviewByAdmin(Long productId, boolean pass, String reason) {
        String normalizedReason = trimToNull(reason);
        if (!pass && normalizedReason == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请输入驳回原因");
        }

        Product product = productMapper.selectById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        requireStatus(product, Set.of(ProductStatus.PENDING_REVIEW.getCode()), "商品不在待审核状态");

        int targetStatus = pass ? ProductStatus.ON_SALE.getCode() : ProductStatus.REJECTED.getCode();
        if (productMapper.reviewByAdmin(productId, targetStatus) == 0) {
            throw conflict("商品状态已变化，请刷新后重试");
        }

        // 审核日志与状态更新属于同一事务，不能出现商品已上架却没有审核记录的情况。
        productMapper.insertReviewLog(
                productId,
                UserContext.requireCurrentUser().userId(),
                pass ? 1 : 2,
                normalizedReason
        );
        invalidateDetailCacheAfterCommit(productId);
    }

    private void validateProductForm(Integer itemCondition, Long categoryId) {
        if (!ItemCondition.isValid(itemCondition)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品成色不正确");
        }
        if (!categoryMapper.existsEnabledById(categoryId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品分类不存在或已停用");
        }
    }

    private Product requireOwnedProduct(Long productId, Long sellerId) {
        Product product = productMapper.selectById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "商品不存在"));
        if (!sellerId.equals(product.getSellerId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该商品");
        }
        return product;
    }

    private void requireEditable(Product product) {
        requireStatus(product, EDITABLE_STATUSES, "当前状态不能编辑商品");
    }

    private void requireStatus(Product product, Set<Integer> allowedStatuses, String message) {
        if (!allowedStatuses.contains(product.getStatus())) {
            throw conflict(message);
        }
    }

    private MyProductVO toMyProductVO(Product product) {
        List<String> images = productMapper.selectImageUrlsByProductId(product.getId());
        String rejectReason = product.getStatus().equals(ProductStatus.REJECTED.getCode())
                ? productMapper.selectLatestRejectReason(product.getId())
                : null;
        return new MyProductVO(
                product.getId(),
                product.getCategoryId(),
                productMapper.selectCategoryNameById(product.getCategoryId()),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getItemCondition(),
                product.getCampus(),
                product.getTradePlace(),
                product.getStatus(),
                product.getVersion(),
                // 项目使用 Java 17，List 尚未提供 getFirst()，非空时取下标 0 作为封面。
                images.isEmpty() ? null : images.get(0),
                images,
                rejectReason,
                product.getCreatedAt()
        );
    }

    private PendingProductVO toPendingProductVO(Product product) {
        List<String> images = productMapper.selectImageUrlsByProductId(product.getId());
        SellerSummary seller = productMapper.selectSellerSummary(product.getSellerId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "卖家不存在"));
        return new PendingProductVO(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getStock(),
                product.getItemCondition(),
                productMapper.selectCategoryNameById(product.getCategoryId()),
                // 图片按 sort 排序，第一张即管理员审核列表展示的封面。
                images.isEmpty() ? null : images.get(0),
                images,
                toSellerVO(seller),
                product.getCreatedAt()
        );
    }

    private ProductSellerVO toSellerVO(SellerSummary seller) {
        return new ProductSellerVO(
                seller.getId(),
                seller.getNickname(),
                seller.getAvatar(),
                seller.getCreditScore(),
                seller.getDealCount(),
                seller.getAvgRating(),
                seller.getGoodReviewRate()
        );
    }

    private RecentReviewVO toRecentReviewVO(ReviewSummary review) {
        return new RecentReviewVO(review.getRating(), review.getContent(), review.getCreatedAt());
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        // DTO 已保证非空和数量上限；这里 trim 后再次过滤防止只包含空格的 URL 入库。
        List<String> normalized = imageUrls.stream()
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请至少上传 1 张商品图片");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    /**
     * 在数据库事务真正提交后再删缓存，避免事务最终回滚却提前删掉仍然有效的旧缓存。
     */
    private void invalidateDetailCacheAfterCommit(Long productId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productDetailCacheService.invalidate(productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productDetailCacheService.invalidate(productId);
            }
        });
    }
}
