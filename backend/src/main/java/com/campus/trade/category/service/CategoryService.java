package com.campus.trade.category.service;

import com.campus.trade.category.mapper.CategoryMapper;
import com.campus.trade.category.vo.CategoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品分类业务服务。
 *
 * <p>即使当前逻辑很简单，仍保留 Service 层，后续增加缓存、
 * 校验或数据组合时不需要把业务塞进 Controller。</p>
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        // Service 可以调用 Mapper，但不感知 HTTP 请求和响应对象。
        this.categoryMapper = categoryMapper;
    }

    /**
     * 获取前台可用的分类。
     *
     * <p>使用只读事务表达该方法不会修改数据库，也便于以后接入读写分离。</p>
     */
    @Transactional(readOnly = true)
    public List<CategoryVO> listEnabledCategories() {
        // Mapper 返回数据库实体，stream.map 将每个实体转换为对外 VO。
        return categoryMapper.selectEnabledCategories().stream()
                // 只暴露 id 和 name，不把 sort、enabled 等内部字段返回给前端。
                .map(category -> new CategoryVO(category.getId(), category.getName()))
                .toList();
    }
}
