package com.campus.trade.order.service;

import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.order.dto.CreateOrderRequest;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.entity.TradeOrderLog;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.order.mq.OrderTimeoutEvent;
import com.campus.trade.order.mq.OrderTimeoutMessagePublisher;
import com.campus.trade.order.vo.OrderCreatedVO;
import com.campus.trade.product.entity.Product;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.ProductDetailCacheService;
import com.campus.trade.user.mapper.UserMapper;
import com.campus.trade.review.mapper.ReviewMapper;
import com.campus.trade.dispute.mapper.DisputeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单核心规则的纯单元测试。
 *
 * <p>所有 Mapper 都由 Mockito 模拟，因此不会连接 MySQL/Redis；
 * 重点验证“先幂等、再写订单、最后条件扣库存”和取消订单回补库存的调用关系。</p>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final long BUYER_ID = 7L;
    private static final long SELLER_ID = 8L;
    private static final long PRODUCT_ID = 101L;
    private static final long ORDER_ID = 201L;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProductDetailCacheService productDetailCacheService;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private DisputeMapper disputeMapper;

    @Mock
    private OrderTimeoutMessagePublisher timeoutMessagePublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // ObjectMapper 在本测试中只处理图片字符串数组，直接使用真实对象即可。
        orderService = new OrderService(
                orderMapper,
                productMapper,
                userMapper,
                productDetailCacheService,
                new ObjectMapper(),
                reviewMapper,
                disputeMapper,
                timeoutMessagePublisher
        );
    }

    @AfterEach
    void tearDown() {
        // 生产环境由拦截器清理 ThreadLocal，单元测试必须自行清理。
        UserContext.clear();
    }

    @Test
    void shouldCreateOrderSnapshotAndAtomicallyDecreaseStock() {
        UserContext.set(new CurrentUser(BUYER_ID, 0, "buyer-token"));
        Product product = saleProduct();
        when(orderMapper.selectByBuyerIdAndRequestId(BUYER_ID, "request-1")).thenReturn(Optional.empty());
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productMapper.selectImageUrlsByProductId(PRODUCT_ID)).thenReturn(List.of("/api/uploads/book.jpg"));
        doAnswer(invocation -> {
            TradeOrder inserted = invocation.getArgument(0);
            inserted.setId(ORDER_ID);
            return 1;
        }).when(orderMapper).insert(any(TradeOrder.class));
        when(productMapper.decreaseStockForOrder(PRODUCT_ID, 2)).thenReturn(1);

        OrderCreatedVO result = orderService.create(createRequest("request-1", 2));

        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING_CONFIRM.getCode());
        ArgumentCaptor<TradeOrder> orderCaptor = ArgumentCaptor.forClass(TradeOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        TradeOrder inserted = orderCaptor.getValue();
        assertThat(inserted.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(inserted.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(inserted.getUnitPrice()).isEqualByComparingTo("88.00");
        assertThat(inserted.getTotalAmount()).isEqualByComparingTo("176.00");
        verify(orderMapper).insertSnapshot(any());
        verify(productMapper).decreaseStockForOrder(PRODUCT_ID, 2);
        verify(productDetailCacheService).invalidate(PRODUCT_ID);
        ArgumentCaptor<OrderTimeoutEvent> eventCaptor = ArgumentCaptor.forClass(OrderTimeoutEvent.class);
        verify(timeoutMessagePublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(ORDER_ID);
        assertThat(eventCaptor.getValue().confirmDeadline()).isEqualTo(inserted.getConfirmDeadline());
    }

    @Test
    void shouldReturnExistingOrderForSameIdempotencyKeyWithoutTouchingStock() {
        UserContext.set(new CurrentUser(BUYER_ID, 0, "buyer-token"));
        TradeOrder existed = new TradeOrder();
        existed.setId(ORDER_ID);
        existed.setOrderNo("O20260725120000000000000000001");
        existed.setStatus(OrderStatus.PENDING_CONFIRM.getCode());
        existed.setConfirmDeadline(LocalDateTime.now().plusHours(20));
        when(orderMapper.selectByBuyerIdAndRequestId(BUYER_ID, "request-1")).thenReturn(Optional.of(existed));

        OrderCreatedVO result = orderService.create(createRequest("request-1", 1));

        assertThat(result.id()).isEqualTo(ORDER_ID);
        verify(productMapper, never()).selectById(any());
        verify(productMapper, never()).decreaseStockForOrder(any(), any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void shouldCancelOrderAndRestoreReservedStock() {
        UserContext.set(new CurrentUser(BUYER_ID, 0, "buyer-token"));
        TradeOrder order = existingOrder(OrderStatus.PENDING_CONFIRM.getCode());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.cancelByParticipant(ORDER_ID, BUYER_ID)).thenReturn(1);

        orderService.cancel(ORDER_ID, "双方协商取消");

        verify(productMapper).restoreStockForCancelledOrder(PRODUCT_ID, 1);
        ArgumentCaptor<TradeOrderLog> logCaptor = ArgumentCaptor.forClass(TradeOrderLog.class);
        verify(orderMapper).insertLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING_CONFIRM.getCode());
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.CANCELLED.getCode());
        assertThat(logCaptor.getValue().getOperatorType()).isZero();
        verify(productDetailCacheService).invalidate(PRODUCT_ID);
    }

    @Test
    void shouldOnlyAllowBuyerToCompleteConfirmedOrder() {
        UserContext.set(new CurrentUser(BUYER_ID, 0, "buyer-token"));
        TradeOrder order = existingOrder(OrderStatus.CONFIRMED.getCode());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.completeByBuyer(ORDER_ID, BUYER_ID)).thenReturn(1);

        orderService.complete(ORDER_ID);

        verify(orderMapper).completeByBuyer(ORDER_ID, BUYER_ID);
        ArgumentCaptor<TradeOrderLog> logCaptor = ArgumentCaptor.forClass(TradeOrderLog.class);
        verify(orderMapper).insertLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getFromStatus()).isEqualTo(OrderStatus.CONFIRMED.getCode());
        assertThat(logCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.COMPLETED.getCode());
        assertThat(logCaptor.getValue().getOperatorType()).isZero();
        verify(userMapper).incrementDealCount(SELLER_ID);
    }

    private CreateOrderRequest createRequest(String requestId, int quantity) {
        return new CreateOrderRequest(
                PRODUCT_ID,
                quantity,
                LocalDateTime.of(2026, 7, 26, 18, 0),
                "图书馆门口",
                "请提前联系",
                requestId
        );
    }

    private Product saleProduct() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSellerId(SELLER_ID);
        product.setTitle("九成新图书");
        product.setDescription("无笔记，保存完好");
        product.setPrice(new BigDecimal("88.00"));
        product.setItemCondition(1);
        product.setCampus("东校区");
        product.setTradePlace("图书馆门口");
        return product;
    }

    private TradeOrder existingOrder(int status) {
        TradeOrder order = new TradeOrder();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setProductId(PRODUCT_ID);
        order.setQuantity(1);
        order.setStatus(status);
        return order;
    }
}
