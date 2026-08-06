package com.campus.trade.dispute.controller;

import com.campus.trade.common.response.Result;
import com.campus.trade.dispute.dto.CreateDisputeRequest;
import com.campus.trade.dispute.service.DisputeService;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易双方使用的纠纷接口，目前只有「发起纠纷」。
 *
 * <p>管理员侧的查询与裁决在 {@link AdminDisputeController}，走 /admin/disputes 前缀，
 * 这样权限可以按路径前缀统一拦截，不必在方法里逐个判断角色。</p>
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
}
