package com.campus.trade.ai.review;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReviewRunMapper {
    int insert(ProductReviewRunEntity run);
    ProductReviewRunEntity selectByRunId(@Param("runId") String runId);
    ProductReviewRunEntity selectLatestByProductId(@Param("productId") Long productId);
    int markRunning(@Param("runId") String runId);
    int markSuccess(@Param("runId") String runId, @Param("decision") String decision,
                    @Param("riskLevel") String riskLevel, @Param("confidence") Double confidence,
                    @Param("resultJson") String resultJson);
    int markFailure(@Param("runId") String runId, @Param("status") String status,
                    @Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);
    int markStale(@Param("runId") String runId, @Param("errorCode") String errorCode);
    List<ProductReviewRunEntity> selectStale(@Param("cutoff") java.time.LocalDateTime cutoff,
                                             @Param("limit") int limit);
}
