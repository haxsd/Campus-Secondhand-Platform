package com.campus.trade.dispute.model;

import java.time.LocalDateTime;

/**
 * 纠纷的业务规则常量与判断，供纠纷模块和订单模块共用。
 *
 * <p>之所以单独抽出来，是因为同一条规则有两个地方要用：DisputeService 在发起纠纷时校验，
 * OrderService 在订单详情里计算 canDispute 标记。两边必须一致，否则会出现
 * “按钮能点但一点就报错”这种前后端体验割裂的问题。</p>
 */
public final class DisputeRules {

    /** 已完成订单的售后窗口：完成超过 7 天后不再受理纠纷。 */
    public static final long AFTER_SALE_WINDOW_DAYS = 7L;

    private DisputeRules() {
    }

    /**
     * 判断已完成订单是否仍在售后窗口内。
     *
     * <p>历史数据可能没有 finished_at，无法判断窗口时选择放行，而不是误伤用户。</p>
     */
    public static boolean withinAfterSaleWindow(LocalDateTime finishedAt) {
        return finishedAt == null
                || !finishedAt.plusDays(AFTER_SALE_WINDOW_DAYS).isBefore(LocalDateTime.now());
    }
}
