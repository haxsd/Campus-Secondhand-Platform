package com.campus.trade.category.service;

import com.campus.trade.category.mapper.CategoryMapper;
import com.campus.trade.category.vo.CategoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类业务服务。
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 获取前台可用的分类。
     *
     * <p>使用只读事务表达该方法不会修改数据库，也便于以后接入读写分离。</p>
     */
    @Transactional(readOnly = true)
    public List<CategoryVO> listEnabledCategories() {
        return categoryMapper.selectEnabledCategories().stream()
                .map(category -> new CategoryVO(category.getId(), category.getName()))
                .toList();
    }
}
