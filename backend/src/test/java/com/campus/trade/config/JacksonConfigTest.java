package com.campus.trade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局 JSON 规则测试：BIGINT 主键不能丢精度，时间字段必须只有一种格式。
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
     * 未加 {@code @JsonFormat} 的 LocalDateTime 也必须输出 yyyy-MM-dd HH:mm:ss，
     * 而不是带 T 的 ISO 格式，否则同一份接口里会出现两种时间格式。
     */
    @Test
    void shouldSerializeLocalDateTimeWithoutIsoT() throws Exception {
        String json = objectMapper.writeValueAsString(
                new TimeSample(LocalDateTime.of(2026, 7, 24, 10, 15, 30)));

        assertThat(json).isEqualTo("{\"createdAt\":\"2026-07-24 10:15:30\"}");
    }

    /** 反序列化同样接受这一种格式，与前端 el-date-picker 的 value-format 对齐。 */
    @Test
    void shouldDeserializeLocalDateTimeFromSameFormat() throws Exception {
        TimeSample sample = objectMapper.readValue(
                "{\"createdAt\":\"2026-07-24 10:15:30\"}", TimeSample.class);

        assertThat(sample.createdAt()).isEqualTo(LocalDateTime.of(2026, 7, 24, 10, 15, 30));
    }

    /**
     * 用超过 JavaScript 安全整数范围的主键做样本，防止回归。
     */
    private record IdSample(Long id) {
    }

    private record TimeSample(LocalDateTime createdAt) {
    }
}
