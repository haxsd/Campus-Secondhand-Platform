package com.campus.trade.order.model;

/**
 * 订单状态编码。
 *
 * <p>编码必须与 trade_order.status 表注释和前端 ORDER_STATUS 常量保持一致。
 * 状态变更只允许由 OrderService 的状态机方法执行，前端不能直接提交 status。</p>
 */
public enum OrderStatus {

    /** 买家已下单，等待卖家确认。 */
    PENDING_CONFIRM(0),

    /** 卖家已确认，等待线下交易完成。 */
    CONFIRMED(1),

    /** 买家确认线下交易完成。 */
    COMPLETED(2),

    /** 买家或卖家主动取消。 */
    CANCELLED(3),

    /** 系统因卖家未及时确认而取消，定时任务阶段实现。 */
    TIMEOUT_CANCELLED(4),

    /** 交易发生争议，纠纷模块阶段实现。 */
    IN_DISPUTE(5);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
