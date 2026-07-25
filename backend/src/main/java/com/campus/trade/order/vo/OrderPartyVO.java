package com.campus.trade.order.vo;

/** 订单中可公开展示的交易方信息；不返回手机号、密码等敏感字段。 */
public record OrderPartyVO(Long id, String nickname, String avatar) {
}
