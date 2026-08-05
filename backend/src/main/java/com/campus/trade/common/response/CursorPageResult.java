package com.campus.trade.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基于游标的分页响应。
 *
 * <p>与需要扫描并跳过大量记录的 OFFSET 分页不同，下一页通过上一页最后一条记录的
 * {@code (createdAt, id)} 定位。当前项目只用于管理端纠纷列表，因此没有改动其他已有的
 * {@link PageResult} 接口。</p>
 *
 * @param list                  当前页数据
 * @param hasNext               是否还有下一页
 * @param nextCursorCreatedAt   请求下一页时应提交的创建时间；没有下一页时为 null
 * @param nextCursorId          请求下一页时应提交的主键；没有下一页时为 null
 * @param <T>                   单条记录类型
 */
public record CursorPageResult<T>(
        List<T> list,
        boolean hasNext,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime nextCursorCreatedAt,
        Long nextCursorId
) {
}
