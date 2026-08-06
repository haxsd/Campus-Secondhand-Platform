package com.campus.trade.dispute.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record AdminDisputeVO(Long id, Long orderId, Integer reasonType, String statement, List<String> evidence,
                             Integer status, Integer evidenceVersion, String orderNo, String productTitle,
                             String buyerName, String sellerName, Integer orderStatus,
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {}
