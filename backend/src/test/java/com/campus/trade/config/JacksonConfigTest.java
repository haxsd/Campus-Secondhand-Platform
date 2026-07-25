package com.campus.trade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 BIGINT 主键不会以 JavaScript 数字形式输出。
 */
@JsonTest
@Import(JacksonConfig.class)
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeLongAsString() throws Exception {
        String json = objectMapper.writeValueAsString(new IdSample(9_007_199_254_740_993L));

        assertThat(json).isEqualTo("{\"id\":\"9007199254740993\"}");
    }

    /**
     * 测试专用对象，数值刻意超过 JavaScript 最大安全整数。
     */
    private record IdSample(Long id) {
    }
}
