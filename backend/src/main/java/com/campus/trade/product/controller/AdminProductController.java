package com.campus.trade.product.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.product.dto.ProductReviewRequest;
import com.campus.trade.product.service.ProductService;
import com.campus.trade.product.vo.PendingProductVO;
import com.campus.trade.product.vo.ProductDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员商品审核接口。
 *
 * <p>路径以 /admin 开头，因此会先经过 LoginInterceptor，
 * 再经过 AdminInterceptor。Controller 可以专注于收发 HTTP 数据，
 * 不需要自行判断当前用户角色。</p>
 */
@Validated
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 查询全站仍处于待审核状态的商品。
     */
    @GetMapping("/pending")
    public Result<List<PendingProductVO>> listPending() {
        return Result.ok(productService.listPending());
    }

    /** 管理员审核前读取待审核商品完整资料，公开详情接口不会返回该状态的商品。 */
    @GetMapping("/{id}")
    public Result<ProductDetailVO> getPendingDetail(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id
    ) {
        return Result.ok(productService.getPendingDetailForAdmin(id));
    }

    /**
     * 审核一件待审核商品。
     *
     * <p>通过后变为在售，驳回后变为审核驳回；每次审核都会写 product_review_log。</p>
     */
    @PostMapping("/{id}/review")
    public Result<Void> review(
            @PathVariable @Min(value = 1, message = "商品 ID 不正确") Long id,
            @Valid @RequestBody ProductReviewRequest request
    ) {
        productService.reviewByAdmin(id, request.pass(), request.reason());
        return Result.ok();
    }
}
