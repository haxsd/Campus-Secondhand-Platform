package com.campus.trade.dispute.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record AppendDisputeEvidenceRequest(
        @Size(max = 2000) String statement,
        @Size(max = 5) List<@Size(max = 255) String> evidence
) {
}
