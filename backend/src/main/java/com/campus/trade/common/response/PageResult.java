package com.campus.trade.common.response;

import java.util.List;

/**
 * 分页查询的统一业务数据结构。
 *
 * <p>它作为 {@link Result} 的 data 使用，例如商品列表最终返回的 JSON 为
 * {@code {"code":0,"message":"ok","data":{"list":[],"total":0,"page":1,"pageSize":10}}}。
 * PageResult 本身不处理 HTTP，只描述一页查询结果。</p>
 *
 * @param list     当前页记录
 * @param total    符合条件的总记录数
 * @param page     当前页码，从 1 开始
 * @param pageSize 每页条数
 * @param <T>      单条记录的类型
 */
public record PageResult<T>(List<T> list, long total, int page, int pageSize) {
}
