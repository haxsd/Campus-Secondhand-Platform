package com.campus.trade.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 序列化的全局配置。
 */
@Configuration
public class JacksonConfig {

    /**
     * 将 Long 和 long 统一序列化为字符串。
     *
     * <p>数据库主键使用 BIGINT，可能超过 JavaScript 的安全整数范围。
     * 在后端统一转成字符串后，商品 ID、用户 ID 和订单相关 ID 都不会在前端丢失精度。</p>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
