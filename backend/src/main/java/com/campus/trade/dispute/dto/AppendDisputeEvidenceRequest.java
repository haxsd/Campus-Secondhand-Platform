package com.campus.trade.dispute.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 纠纷参与方补充材料请求。补充内容会追加到原证据，不会覆盖历史证据。
 */
public record AppendDisputeEvidenceRequest(
        @Size(max = 2000) String statement,
        @Size(max = 5) List<@Size(max = 255) String> evidence
) {
}
