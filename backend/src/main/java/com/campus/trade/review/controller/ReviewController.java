package com.campus.trade.review.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.review.dto.CreateReviewRequest;
import com.campus.trade.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价接口。
 *
 * <p>目前只有「提交评价」一个写接口；卖家的评分与最近评价由商品详情接口一并返回，
 * 因此没有单独的评价查询接口。</p>
 */
@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateReviewRequest request) {
        reviewService.create(request);
        return Result.ok();
    }
}
