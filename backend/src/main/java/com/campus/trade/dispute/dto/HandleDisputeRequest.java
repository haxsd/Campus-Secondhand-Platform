package com.campus.trade.dispute.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HandleDisputeRequest(
        @NotBlank @Pattern(regexp = "REJECT|KEEP_COMPLETED|CANCEL_TRADE|NEED_MORE") String action,
        Boolean restock,
        @Size(max = 1000) String note,
        @NotNull @Min(1) Integer evidenceVersion
) {
}
