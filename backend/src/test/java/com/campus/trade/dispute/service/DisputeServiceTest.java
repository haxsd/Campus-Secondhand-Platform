package com.campus.trade.dispute.service;

import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.dispute.dto.CreateDisputeRequest;
import com.campus.trade.dispute.mapper.DisputeEvidenceLogMapper;
import com.campus.trade.dispute.mapper.DisputeMapper;
import com.campus.trade.dispute.model.AdminDisputeRow;
import com.campus.trade.dispute.vo.AdminDisputeVO;
import com.campus.trade.dispute.vo.DisputeCreatedVO;
import com.campus.trade.order.entity.TradeOrder;
import com.campus.trade.order.mapper.OrderMapper;
import com.campus.trade.order.model.OrderStatus;
import com.campus.trade.product.mapper.ProductMapper;
import com.campus.trade.product.service.SellerDetailCacheInvalidator;
import com.campus.trade.review.mapper.ReviewMapper;
import com.campus.trade.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 纠纷发起环节的单元测试，重点是售后时间窗口这条业务规则。
 */
@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    private static final long BUYER_ID = 11L;
    private static final long SELLER_ID = 22L;
    private static final long ORDER_ID = 101L;

    @Mock
    private DisputeEvidenceLogMapper evidenceLogMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private DisputeMapper disputeMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SellerDetailCacheInvalidator cacheInvalidator;

    private DisputeService disputeService;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(
                disputeMapper,
                evidenceLogMapper,
                orderMapper,
                productMapper,
                userMapper,
                reviewMapper,
                cacheInvalidator,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldCreateDisputeForRecentlyCompletedOrder() {
        UserContext.set(new CurrentUser(BUYER_ID, 0, "token-buyer"));
        TradeOrder order = completedOrder(LocalDateTime.now().minusDays(3));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(Optional.of(order));
        when(disputeMapper.existsByOrderId(ORDER_ID)).thenReturn(false);
        doAnswer(invocation -> {
            invocation.<com.campus.trade.dispute.entity.Dispute>getArgument(0).setId(555L);
            return 1;
        }).when(disputeMapper).insert(any());
        when(orderMapper.enterDispute(anyLong(), anyLong(), anyInt())).thenReturn(1);

        DisputeCreatedVO created = disputeService.create(request());

        assertThat(created.id()).isEqualTo(555L);
        verify(orderMapper).enterDispute(ORDER_ID, BUYER_ID, OrderStatus.COMPLETED.getCode());
    }

    @Test
    void shouldRejectDisputeAfterSevenDaysSinceCompletion() {
        UserContext.set(new CurrentUser(BUYER_ID, 0, "token-buyer"));
        // 完成于 8 天前，已超出售后窗口。
        when(orderMapper.selectById(ORDER_ID))
                .thenReturn(Optional.of(completedOrder(LocalDateTime.now().minusDays(8))));

        assertThatThrownBy(() -> disputeService.create(request()))
                .isInstanceOf(BizException.class)
                .hasMessage("订单完成已超过 7 天，不能再发起纠纷")
                .extracting(exception -> ((BizException) exception).getCode())
                .isEqualTo(409);

        verify(disputeMapper, never()).insert(any());
    }

    @Test
    void shouldAllowDisputeOnConfirmedOrderRegardlessOfTime() {
        UserContext.set(new CurrentUser(SELLER_ID, 0, "token-seller"));
        TradeOrder order = completedOrder(null);
        // 已确认但尚未完成的订单不受售后窗口限制，线下交易还在进行中。
        order.setStatus(OrderStatus.CONFIRMED.getCode());
        order.setCreatedAt(LocalDateTime.now().minusDays(30));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(Optional.of(order));
        when(disputeMapper.existsByOrderId(ORDER_ID)).thenReturn(false);
        doAnswer(invocation -> {
            invocation.<com.campus.trade.dispute.entity.Dispute>getArgument(0).setId(556L);
            return 1;
        }).when(disputeMapper).insert(any());
        when(orderMapper.enterDispute(anyLong(), anyLong(), anyInt())).thenReturn(1);

        assertThat(disputeService.create(request()).id()).isEqualTo(556L);
    }

    @Test
    void shouldRejectDisputeFromNonParticipant() {
        UserContext.set(new CurrentUser(99L, 0, "token-other"));
        when(orderMapper.selectById(ORDER_ID))
                .thenReturn(Optional.of(completedOrder(LocalDateTime.now())));

        assertThatThrownBy(() -> disputeService.create(request()))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getCode())
                .isEqualTo(403);
    }

    @Test
    void shouldBuildAdminListFromJoinResultWithoutPerRowQueries() {
        AdminDisputeRow row = new AdminDisputeRow();
        row.setId(701L);
        row.setOrderId(ORDER_ID);
        row.setReasonType(0);
        row.setStatement("商品与描述不符");
        row.setEvidenceJson("[\"/api/uploads/evidence.jpg\"]");
        row.setStatus(0);
        row.setOrderNo("O202607270001");
        row.setProductTitle("机械键盘");
        row.setBuyerName("买家A");
        row.setSellerName("卖家B");
        row.setOrderStatus(OrderStatus.IN_DISPUTE.getCode());
        row.setCreatedAt(LocalDateTime.now());
        when(disputeMapper.selectAdminCursorPage(0, null, null, 21)).thenReturn(List.of(row));

        var page = disputeService.list(0, null, null, 20);

        AdminDisputeVO result = page.list().get(0);
        assertThat(result.orderNo()).isEqualTo("O202607270001");
        assertThat(result.productTitle()).isEqualTo("机械键盘");
        assertThat(result.evidence()).containsExactly("/api/uploads/evidence.jpg");
        assertThat(page.hasNext()).isFalse();
        // 订单、商品和用户数据已经由 JOIN SQL 返回，Service 不应再执行逐行查询。
        verifyNoInteractions(orderMapper, productMapper, userMapper);
    }

    @Test
    void shouldUseLastReturnedRowAsNextCursor() {
        LocalDateTime newestTime = LocalDateTime.of(2026, 7, 27, 12, 0);
        AdminDisputeRow newest = new AdminDisputeRow();
        newest.setId(702L);
        newest.setCreatedAt(newestTime);
        AdminDisputeRow probeRow = new AdminDisputeRow();
        probeRow.setId(701L);
        probeRow.setCreatedAt(newestTime.minusSeconds(1));
        // pageSize=1 时多取一条，第二条只用于判断还有下一页，不能作为下一页游标。
        when(disputeMapper.selectAdminCursorPage(0, null, null, 2))
                .thenReturn(List.of(newest, probeRow));

        var page = disputeService.list(0, null, null, 1);

        assertThat(page.list()).hasSize(1);
        assertThat(page.list().get(0).id()).isEqualTo(702L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursorCreatedAt()).isEqualTo(newestTime);
        assertThat(page.nextCursorId()).isEqualTo(702L);
    }

    private CreateDisputeRequest request() {
        return new CreateDisputeRequest(ORDER_ID, 0, "  收到的商品与描述不符  ", List.of("/api/uploads/e1.jpg"));
    }

    private TradeOrder completedOrder(LocalDateTime finishedAt) {
        TradeOrder order = new TradeOrder();
        order.setId(ORDER_ID);
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setProductId(301L);
        order.setQuantity(1);
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setFinishedAt(finishedAt);
        return order;
    }
}
