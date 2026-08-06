package com.campus.trade.dispute.service;

import com.campus.trade.common.context.CurrentUser;
import com.campus.trade.common.context.UserContext;
import com.campus.trade.common.exception.BizException;
import com.campus.trade.dispute.dto.AppendDisputeEvidenceRequest;
import com.campus.trade.dispute.entity.Dispute;
import com.campus.trade.dispute.mapper.DisputeEvidenceLogMapper;
import com.campus.trade.dispute.mapper.DisputeMapper;
import com.campus.trade.order.mapper.OrderMapper;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeEvidenceAppendTest {
    @Mock DisputeMapper disputes;
    @Mock DisputeEvidenceLogMapper logs;
    @Mock OrderMapper orders;
    @Mock ProductMapper products;
    @Mock UserMapper users;
    @Mock ReviewMapper reviews;
    @Mock SellerDetailCacheInvalidator cacheInvalidator;

    private DisputeService service;

    @BeforeEach
    void setUp() {
        service = new DisputeService(disputes, logs, orders, products, users, reviews, cacheInvalidator, new ObjectMapper());
        UserContext.set(new CurrentUser(11L, 0, "token"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void statusMustBeNeedMoreBeforeAppend() {
        when(disputes.selectById(1L)).thenReturn(Optional.of(dispute(0, List.of())));
        assertThatThrownBy(() -> service.appendEvidence(1L, request()))
                .isInstanceOf(BizException.class);
        verify(disputes, never()).appendEvidence(anyLong(), anyInt(), any());
    }

    @Test
    void appendUsesVersionAndKeepsOldEvidence() {
        when(disputes.selectById(1L)).thenReturn(Optional.of(dispute(1, List.of("old.jpg"))));
        when(disputes.appendEvidence(1L, 1, "[\"old.jpg\",\"new.jpg\"]")).thenReturn(1);
        service.appendEvidence(1L, request());
        verify(disputes).appendEvidence(1L, 1, "[\"old.jpg\",\"new.jpg\"]");
        verify(logs).insert(any());
    }

    @Test
    void concurrentAppendOnlyOneConditionalUpdateSucceeds() {
        when(disputes.selectById(1L)).thenReturn(Optional.of(dispute(1, List.of())));
        when(disputes.appendEvidence(anyLong(), anyInt(), any())).thenReturn(1, 0);
        service.appendEvidence(1L, request());
        assertThatThrownBy(() -> service.appendEvidence(1L, request()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void moreThanFiveImagesPerAppendIsRejected() {
        when(disputes.selectById(1L)).thenReturn(Optional.of(dispute(1, List.of("1", "2", "3", "4", "5"))));
        assertThatThrownBy(() -> service.appendEvidence(1L, new AppendDisputeEvidenceRequest("说明", List.of("6"))))
                .isInstanceOf(BizException.class);
    }

    @Test
    void nonParticipantCannotViewOrAppend() {
        UserContext.set(new CurrentUser(99L, 0, "token"));
        when(disputes.selectById(1L)).thenReturn(Optional.of(dispute(1, List.of())));
        assertThatThrownBy(() -> service.getForParticipant(1L)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.appendEvidence(1L, request())).isInstanceOf(BizException.class);
    }

    private AppendDisputeEvidenceRequest request() {
        return new AppendDisputeEvidenceRequest("补充说明", List.of("new.jpg"));
    }

    private Dispute dispute(int status, List<String> evidence) {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setOrderId(101L);
        dispute.setApplicantId(11L);
        dispute.setRespondentId(22L);
        dispute.setStatus(status);
        dispute.setEvidenceVersion(1);
        try {
            dispute.setEvidenceJson(new ObjectMapper().writeValueAsString(evidence));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return dispute;
    }

    @Test
    void adminDetailUsesFixedNumberOfAggregateQueries() {
        when(disputes.selectById(1L)).thenReturn(Optional.of(dispute(1, List.of())));
        when(orders.selectById(101L)).thenReturn(Optional.empty());
        when(users.selectById(11L)).thenReturn(Optional.empty());
        when(users.selectById(22L)).thenReturn(Optional.empty());
        when(users.selectCreditSummary(11L)).thenReturn(Optional.empty());
        when(users.selectCreditSummary(22L)).thenReturn(Optional.empty());
        when(logs.selectByDisputeId(1L)).thenReturn(List.of());

        service.adminDetail(1L);

        verify(orders).selectById(101L);
        verify(users).selectById(11L);
        verify(users).selectById(22L);
        verify(users).selectCreditSummary(11L);
        verify(users).selectCreditSummary(22L);
        verify(logs).selectByDisputeId(1L);
    }
}
