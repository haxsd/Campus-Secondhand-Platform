package com.campus.trade.order.mq;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单确认超时事件。
 *
 * <p>消息只携带定位订单和排查问题所需的最小字段，不复制价格、库存等业务事实。
 * 消费时必须重新查询 MySQL，并以订单当前状态决定是否允许关单。</p>
 *
 * @param eventId         事件唯一编号，用于日志追踪
 * @param orderId         待检查的订单 ID
 * @param confirmDeadline 该订单的卖家确认截止时间
 * @param schemaVersion   消息结构版本，便于以后兼容字段调整
 */
public record OrderTimeoutEvent(
        String eventId,
        Long orderId,
        LocalDateTime confirmDeadline,
        int schemaVersion
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    /**
     * 创建当前版本的超时事件。
     */
    public static OrderTimeoutEvent create(Long orderId, LocalDateTime confirmDeadline) {
        return new OrderTimeoutEvent(
                UUID.randomUUID().toString(),
                orderId,
                confirmDeadline,
                CURRENT_SCHEMA_VERSION
        );
    }

    /**
     * 校验消息中的必要字段。
     *
     * <p>该校验只能判断消息结构是否可用，不能判断订单是否应该取消；
     * 订单状态和截止时间必须由数据库条件更新进行最终确认。</p>
     */
    public void validate() {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId 不能为空");
        }
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId 不正确");
        }
        if (confirmDeadline == null) {
            throw new IllegalArgumentException("confirmDeadline 不能为空");
        }
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("不支持的消息结构版本: " + schemaVersion);
        }
    }
}
