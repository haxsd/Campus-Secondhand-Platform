package com.campus.trade.dispute.controller;

import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.CursorPageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.HandleDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理端纠纷接口：查看纠纷列表与裁决。
 *
 * <p>这里不需要写任何权限判断：{@code /admin/**} 已经由 AdminInterceptor 统一拦截，
 * 非管理员在进入 Controller 之前就会收到 403。</p>
 */
@Validated
@RestController
@RequestMapping("/admin/disputes")
public class AdminDisputeController {

    private final DisputeService disputeService;

    public AdminDisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * 游标分页查看纠纷，可按纠纷状态 0~4 过滤。
     *
     * <p>首次请求不传游标；下一页同时传入上一页响应中的 cursorCreatedAt 和 cursorId。
     * 两个字段缺一不可，避免后端按不稳定条件翻页。</p>
     */
    @GetMapping
    public Result<CursorPageResult<AdminDisputeVO>> list(
            @RequestParam(required = false) @Min(0) @Max(4) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) @Min(1) Long cursorId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize
    ) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "游标时间和游标 ID 必须同时传入");
        }
        return Result.ok(disputeService.list(status, cursorCreatedAt, cursorId, pageSize));
    }

    /** 裁决纠纷，具体动作见 HandleDisputeRequest.action。 */
    @PostMapping("/{id}/handle")
    public Result<Void> handle(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody HandleDisputeRequest request
    ) {
        disputeService.handle(id, request);
        return Result.ok();
    }
}
