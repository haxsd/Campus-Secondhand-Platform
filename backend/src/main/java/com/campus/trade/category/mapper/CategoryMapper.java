package com.campus.trade.category.mapper;

import com.campus.trade.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品分类持久层。
 *
 * <p>这里只定义方法签名，SQL 位于 CategoryMapper.xml。
 * Mapper 不负责统一响应、权限或其他业务逻辑。</p>
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
