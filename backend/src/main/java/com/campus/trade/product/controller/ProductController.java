package com.campus.trade.product.controller;

import com.campus.trade.common.response.PageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.ai.review.ProductReviewRunService;
import com.campus.trade.ai.review.ProductReviewRunVO;
import com.campus.trade.ai.review.ProductReviewSubmitVO;
import com.campus.trade.product.dto.CreateProductRequest;
import com.campus.trade.product.dto.StockAdjustRequest;
import com.campus.trade.product.dto.UpdateProductRequest;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.vo.MyProductVO;
import com.campus.trade.product.vo.ProductDetailVO;
import com.campus.trade.product.vo.ProductIdVO;
import com.campus.trade.product.vo.ProductListVO;
import com.campus.trade.common.context.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品公开查询与卖家管理接口。
 *
 * <p>同一个 /products 路径下同时存在公开 GET 与登录后写操作。
 * PublicRequestMatcher 按“HTTP 方法 + 精确路径”判断公开性，
 * 因此 POST、PUT 和 /mine 都会经过 LoginInterceptor 认证。</p>
 */
@Validated
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductReviewRunService productReviewRunService;

    public ProductController(ProductService productService, ProductReviewRunService productReviewRunService) {
        this.productService = productService;
        this.productReviewRunService = productReviewRunService;
    }

    /**
     * 查询公开商品列表，只返回在售且库存大于零的商品。
     */
    @GetMapping
    public Result<PageResult<ProductListVO>> listPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "最低价格不能小于 0") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.00", message = "最高价格不能小于 0") BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数至少为 1") @Max(value = 50, message = "每页条数不能超过 50") int pageSize
    ) {
        return Result.ok(productService.listPublic(
                keyword, categoryId, campus, minPrice, maxPrice, page, pageSize
        ));
    }

    /**
     * 查询一件公开商品的完整详情。
     */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getPublicDetail(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        return Result.ok(productService.getPublicDetail(id));
    }

    /**
     * 查询当前登录卖家的全部商品，草稿和审核驳回商品也会返回。
     */
    @GetMapping("/mine")
    public Result<List<MyProductVO>> listMine(
            @RequestParam(required = false) Integer status
    ) {
        return Result.ok(productService.listMine(status));
    }

    /**
     * 创建商品草稿。卖家身份取自 JWT，不接收 sellerId 请求字段。
     */
    @PostMapping
    public Result<ProductIdVO> create(@Valid @RequestBody CreateProductRequest request) {
        return Result.ok(productService.create(request));
    }

    /**
     * 编辑自己的草稿、驳回或已下架商品。
     */
    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        productService.update(id, request);
        return Result.ok();
    }

    /**
     * 卖家申请上架，商品状态进入待审核。
     */
    @PostMapping("/{id}/submit-review")
    public Result<ProductReviewSubmitVO> submitReview(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        return Result.ok(productReviewRunService.submit(id, UserContext.requireCurrentUser().userId()));
    }

    @GetMapping("/{productId}/ai-review-runs/{runId}")
    public Result<ProductReviewRunVO> getAiReviewRun(
            @PathVariable @Min(value = 1, message = "商品 ID 不合法") Long productId,
            @PathVariable String runId
    ) {
        ProductReviewRunVO result = productReviewRunService.getRun(runId);
        if (!productId.equals(result.productId())) {
            throw new com.campus.trade.common.exception.BizException(
                    com.campus.trade.common.exception.ErrorCode.NOT_FOUND, "审核运行不存在");
        }
        return Result.ok(result);
    }

    @GetMapping("/{productId}/ai-review-runs/latest")
    public Result<ProductReviewRunVO> getLatestAiReviewRun(
            @PathVariable @Min(value = 1, message = "商品 ID 不合法") Long productId
    ) {
        return Result.ok(productReviewRunService.getLatestRun(productId));
    }

    @GetMapping("/{id}/ai-review")
    public Result<com.campus.trade.ai.review.AdminProductReviewVO> getSellerAiReview(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        return Result.ok(productReviewRunService.getSellerReview(
                id, UserContext.requireCurrentUser().userId()));
    }

    @PostMapping("/{id}/request-manual-review")
    public Result<Void> requestManualReview(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        productReviewRunService.requestManualReview(id, UserContext.requireCurrentUser().userId());
        return Result.ok();
    }

    /**
     * 卖家撤回待审核申请，商品状态回到草稿。
     */
    @PostMapping("/{id}/withdraw-review")
    public Result<Void> withdrawReview(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        productService.withdrawReview(id);
        return Result.ok();
    }

    /**
     * 下架自己的在售商品。
     */
    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        productService.offShelf(id);
        return Result.ok();
    }

    /**
     * 在售商品增减库存。库存减少后由 Service 保证至少剩余一件。
     */
    @PostMapping("/{id}/stock")
    public Result<Void> adjustStock(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id,
            @Valid @RequestBody StockAdjustRequest request
    ) {
        productService.adjustStock(id, request);
        return Result.ok();
    }
}
