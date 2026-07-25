package com.campus.trade.history.controller;

import com.campus.trade.common.response.PageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.history.service.BrowseHistoryService;
import com.campus.trade.history.vo.BrowseHistoryVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的浏览记录查询接口。
 *
 * <p>写入不通过本 Controller 暴露，统一由商品详情服务完成，避免前端伪造任意浏览记录。</p>
 */
@Validated
@RestController
@RequestMapping("/browse-history")
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    public BrowseHistoryController(BrowseHistoryService browseHistoryService) {
        this.browseHistoryService = browseHistoryService;
    }

    /**
     * 按最后浏览时间倒序分页查询当前用户记录。
     */
    @GetMapping
    public Result<PageResult<BrowseHistoryVO>> listMine(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从 1 开始") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数至少为 1") @Max(value = 50, message = "每页条数不能超过 50") int pageSize
    ) {
        return Result.ok(browseHistoryService.listMine(page, pageSize));
    }
}
