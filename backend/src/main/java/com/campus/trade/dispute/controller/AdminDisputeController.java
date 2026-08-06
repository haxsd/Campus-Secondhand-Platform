package com.campus.trade.dispute.controller;

import com.campus.trade.common.exception.BizException;
import com.campus.trade.common.exception.ErrorCode;
import com.campus.trade.common.response.CursorPageResult;
import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.HandleDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/admin/disputes")
public class AdminDisputeController {
    private final DisputeService disputeService;
    public AdminDisputeController(DisputeService disputeService) { this.disputeService = disputeService; }
    @GetMapping
    public Result<CursorPageResult<AdminDisputeVO>> list(@RequestParam(required=false) @Min(0) @Max(4) Integer status,
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime cursorCreatedAt,
            @RequestParam(required=false) @Min(1) Long cursorId, @RequestParam(defaultValue="10") @Min(1) @Max(50) int pageSize) {
        if ((cursorCreatedAt == null) != (cursorId == null)) throw new BizException(ErrorCode.BAD_REQUEST, "游标时间和游标 ID 必须同时传入");
        return Result.ok(disputeService.list(status, cursorCreatedAt, cursorId, pageSize));
    }
    @GetMapping("/{id}")
    public Result<DisputeDetailVO> detail(@PathVariable @Min(1) Long id) { return Result.ok(disputeService.adminDetail(id)); }
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable @Min(1) Long id, @Valid @RequestBody HandleDisputeRequest request) {
        disputeService.handle(id, request);
        return Result.ok();
    }
}
