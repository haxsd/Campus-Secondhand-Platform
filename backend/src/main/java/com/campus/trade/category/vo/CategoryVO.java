package com.campus.trade.category.vo;

/**
 * 分类列表返回项。
 *
 * @param id   分类 ID，经过全局 Jackson 配置后以字符串输出
 * @param name 分类名称
 */
public record CategoryVO(Long id, String name) {
}
