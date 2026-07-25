package com.campus.trade.product.model;

import java.math.BigDecimal;

/**
 * 商品公开列表的查询条件。
 *
 * <p>这是 Service 传给 Mapper 的内部对象，不是直接暴露给前端的 DTO。
 * offset 已经由 Service 根据 page 和 pageSize 计算完成，SQL 不需要理解分页规则。</p>
 */
public record ProductSearchCriteria(
        String keyword,
        Long categoryId,
        String campus,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int offset,
        int pageSize
) {
}
