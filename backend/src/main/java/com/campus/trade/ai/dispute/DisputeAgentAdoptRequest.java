package com.campus.trade.ai.dispute;

import jakarta.validation.constraints.NotBlank;

/** 管理员采纳建议时提交的动作，仅记录审计信息，不触发裁决。 */
public record DisputeAgentAdoptRequest(@NotBlank String action) {
}
