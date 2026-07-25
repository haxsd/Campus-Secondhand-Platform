package com.campus.trade.history.service;

import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.response.PageResult;
import com.campus.trade.history.mapper.BrowseHistoryMapper;
import com.campus.trade.history.vo.BrowseHistoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 浏览记录业务服务。
 *
 * <p>商品详情是公开接口，因此当前请求可能没有登录用户。只有 LoginInterceptor 在可选认证后
 * 成功写入 UserContext 时，本类才写数据库；匿名访问详情不产生浏览记录。</p>
 */
@Service
public class BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;

    public BrowseHistoryService(BrowseHistoryMapper browseHistoryMapper) {
        this.browseHistoryMapper = browseHistoryMapper;
    }

    /**
     * 若当前请求已认证则记录浏览；匿名请求直接跳过。
     */
    @Transactional
    public void recordIfAuthenticated(Long productId) {
        UserContext.get().ifPresent(user -> browseHistoryMapper.upsert(user.userId(), productId));
    }

    /**
     * 分页查询当前用户的浏览记录。
     */
    @Transactional(readOnly = true)
    public PageResult<BrowseHistoryVO> listMine(int page, int pageSize) {
        Long userId = UserContext.requireCurrentUser().userId();
        int offset = (page - 1) * pageSize;
        return new PageResult<>(
                browseHistoryMapper.selectPage(userId, offset, pageSize),
                browseHistoryMapper.countByUserId(userId),
                page,
                pageSize
        );
    }
}
