package com.campus.trade.dispute.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发起纠纷的请求体。
 *
 * <p>发起人是谁由 token 决定，因此这里没有 applicantId 字段；
 * 同理也没有被申请人，服务端会取交易的另一方。</p>
 *
 * @param reasonType 纠纷原因：0 货不对板、1 未履约、2 其它
 * @param evidence   证据图片地址列表，最多 5 张，必须是本平台上传接口返回的地址
 */
public record CreateDisputeRequest(
        @NotNull @Min(1) Long orderId,
        @NotNull @Min(0) @Max(2) Integer reasonType,
        @NotBlank @Size(max = 2000) String statement,
        @Size(max = 5) List<@NotBlank @Size(max = 255) String> evidence
) {
}
