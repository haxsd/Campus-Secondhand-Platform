package com.campus.trade.dispute.dto;
import jakarta.validation.constraints.*;
/** 管理员裁决请求；action 由 Service 白名单解析，禁止前端直接传状态编码。 */
public record HandleDisputeRequest(@NotBlank @Pattern(regexp="REJECT|KEEP_COMPLETED|CANCEL_TRADE|NEED_MORE") String action,Boolean restock,@Size(max=1000) String note){}
