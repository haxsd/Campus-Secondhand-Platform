package com.campus.trade.dispute.controller;

import com.campus.trade.common.response.PageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.HandleDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /** 分页查看纠纷，可按纠纷状态 0~4 过滤。 */
    @GetMapping
    public Result<PageResult<AdminDisputeVO>> list(
            @RequestParam(required = false) @Min(0) @Max(4) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize
    ) {
        return Result.ok(disputeService.list(status, page, pageSize));
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
