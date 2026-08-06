package com.campus.trade.dispute.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.AppendDisputeEvidenceRequest;
import com.campus.trade.dispute.dto.CreateDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import com.campus.trade.dispute.vo.DisputeDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/disputes")
public class DisputeController {
    private final DisputeService disputeService;
    public DisputeController(DisputeService disputeService) { this.disputeService = disputeService; }
    @PostMapping
    public Result<DisputeCreatedVO> create(@Valid @RequestBody CreateDisputeRequest request) { return Result.ok(disputeService.create(request)); }
    @GetMapping("/by-order/{orderId}")
    public Result<DisputeDetailVO> byOrder(@PathVariable @Min(1) Long orderId) { return Result.ok(disputeService.getForOrder(orderId)); }
    @GetMapping("/{id}")
    public Result<DisputeDetailVO> detail(@PathVariable @Min(1) Long id) { return Result.ok(disputeService.getForParticipant(id)); }
    @PostMapping("/{id}/evidence")
    public Result<Void> appendEvidence(@PathVariable @Min(1) Long id, @Valid @RequestBody AppendDisputeEvidenceRequest request) { disputeService.appendEvidence(id, request); return Result.ok(); }
}
