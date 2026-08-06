package com.campus.trade.dispute.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.AppendDisputeEvidenceRequest;
import com.campus.trade.dispute.dto.CreateDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import com.campus.trade.dispute.vo.ParticipantDisputeDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 娴溿倖妲楅崣灞炬煙娴ｈ法鏁ら惃鍕眰缁鹃攱甯撮崣锝忕礉閻╊喖澧犻崣顏呮箒閵嗗苯褰傜挧椋庣眰缁炬灚鈧秲鈧?
 *
 * <p>缁狅紕鎮婇崨妯规櫠閻ㄥ嫭鐓＄拠顫瑢鐟佷礁鍠呴崷?{@link AdminDisputeController}閿涘矁铔?/admin/disputes 閸撳秶绱戦敍?
 * 鏉╂瑦鐗遍弶鍐閸欘垯浜掗幐澶庣熅瀵板嫬澧犵紓鈧紒鐔剁閹凤附鍩呴敍灞肩瑝韫囧懎婀弬瑙勭《闁插矂鈧劒閲滈崚銈嗘焽鐟欐帟澹婇妴?/p>
 */
@RestController
@RequestMapping("/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public Result<DisputeCreatedVO> create(@Valid @RequestBody CreateDisputeRequest request) {
        return Result.ok(disputeService.create(request));
    }

    @GetMapping("/{id}")
    public Result<ParticipantDisputeDetailVO> detail(@PathVariable @Min(1) Long id) {
        return Result.ok(disputeService.getForParticipant(id));
    }

    @GetMapping("/by-order/{orderId}")
    public Result<ParticipantDisputeDetailVO> byOrder(@PathVariable @Min(1) Long orderId) {
        return Result.ok(disputeService.getForOrder(orderId));
    }

    @PostMapping("/{id}/evidence")
    public Result<Void> appendEvidence(@PathVariable @Min(1) Long id, @Valid @RequestBody AppendDisputeEvidenceRequest request) {
        disputeService.appendEvidence(id, request);
        return Result.ok();
    }
}
