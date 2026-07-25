package com.campus.trade.category.mapper;

import com.campus.trade.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品分类持久层。
 */
@Mapper
public interface CategoryMapper {

    /**
     * 查询全部启用分类，按照后台配置顺序稳定排序。
     *
     * @return 启用的分类列表
     */
    List<Category> selectEnabledCategories();
}
