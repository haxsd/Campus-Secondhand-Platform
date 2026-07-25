package com.campus.trade.dispute.dto;
import jakarta.validation.constraints.*; import java.util.List;
/** 交易任一方发起纠纷的输入。 */
public record CreateDisputeRequest(@NotNull @Min(1) Long orderId,@NotNull @Min(0) @Max(2) Integer reasonType,@NotBlank @Size(max=2000) String statement,@Size(max=5) List<@NotBlank @Size(max=255) String> evidence){}
