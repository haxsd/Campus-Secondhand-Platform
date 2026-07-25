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
 *
 * <p>这是最简单的标准调用链示例：
 * HTTP GET → CategoryController → CategoryService → CategoryMapper → MySQL。</p>
 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        // Controller 只依赖 Service，不直接调用 Mapper。
        this.categoryService = categoryService;
    }

    /**
     * 查询启用中的分类，无需登录。
     */
    @GetMapping
    public Result<List<CategoryVO>> listEnabledCategories() {
        // 统一使用 Result.ok，前端最终拿到的 data 是分类数组。
        return Result.ok(categoryService.listEnabledCategories());
    }
}
