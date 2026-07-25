package com.campus.trade.product.vo;

/**
 * 新建商品成功后返回的最小数据。
 *
 * @param id 新创建的商品主键；Jackson 会统一把 Long 序列化为字符串
 */
public record ProductIdVO(Long id) {
}
