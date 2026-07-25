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

    /**
     * 判断分类是否存在且处于启用状态。
     *
     * <p>发布商品时只允许选择启用分类，不能只相信前端下拉框，
     * 因为请求可以被手工伪造，分类也可能在用户填写表单期间被停用。</p>
     *
     * @param id 分类主键
     * @return 分类存在且启用时返回 true
     */
    boolean existsEnabledById(Long id);
}
