package com.campus.trade.history.mapper;

import com.campus.trade.history.vo.BrowseHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户浏览记录的数据访问接口。
 *
 * <p>浏览记录有 user_id + product_id 唯一索引，重复浏览不会创建多条记录，
 * 而是更新 last_browsed_at 并在列表中移动到最前面。</p>
 */
@Mapper
public interface BrowseHistoryMapper {

    /**
     * 写入或更新一条浏览记录。
     */
    int upsert(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 查询当前页浏览记录，按最后浏览时间倒序。 */
    List<BrowseHistoryVO> selectPage(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /** 查询当前用户可展示浏览记录的总数。 */
    long countByUserId(@Param("userId") Long userId);
}
