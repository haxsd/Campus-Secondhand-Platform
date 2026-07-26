package com.campus.trade.dispute.vo;

/** 发起纠纷成功后返回的纠纷 id（序列化为字符串，避免前端精度丢失）。 */
public record DisputeCreatedVO(Long id) {
}
