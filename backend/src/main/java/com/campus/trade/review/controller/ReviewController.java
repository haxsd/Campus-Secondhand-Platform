package com.campus.trade.review.controller;
import com.campus.trade.common.response.Result; import com.campus.trade.review.dto.CreateReviewRequest; import com.campus.trade.review.service.ReviewService; import jakarta.validation.Valid; import org.springframework.web.bind.annotation.*;
/** 买家评价接口。 */
@RestController @RequestMapping("/reviews") public class ReviewController { private final ReviewService service; public ReviewController(ReviewService service){this.service=service;} @PostMapping public Result<Void> create(@Valid @RequestBody CreateReviewRequest request){service.create(request);return Result.ok();} }
