package com.campus.trade.category.controller;

import com.campus.trade.category.service.CategoryService;
import com.campus.trade.category.vo.CategoryVO;
import com.campus.trade.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类公开接口。
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 查询启用中的分类，无需登录。
     */
    @GetMapping
    public Result<List<CategoryVO>> listEnabledCategories() {
        return Result.ok(categoryService.listEnabledCategories());
    }
}
