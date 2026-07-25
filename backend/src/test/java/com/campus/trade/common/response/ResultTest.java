package com.campus.trade.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应结构的快速单元测试。
 */
class ResultTest {

    @Test
    void shouldCreateSuccessResultWithData() {
        Result<String> result = Result.ok("data");

        assertThat(result.code()).isZero();
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.data()).isEqualTo("data");
    }

    @Test
    void shouldCreateFailureResultWithoutData() {
        Result<Void> result = Result.fail(409, "状态冲突");

        assertThat(result.code()).isEqualTo(409);
        assertThat(result.message()).isEqualTo("状态冲突");
        assertThat(result.data()).isNull();
    }
}
