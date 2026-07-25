package com.campus.trade.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 序列化的全局配置。
 *
 * <p>序列化是“Java 对象 → JSON”，反序列化是“JSON → Java 对象”。
 * 把规则集中在这里后，每个 VO 都不需要重复添加 Long 转字符串注解。</p>
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
            // Long.class 处理包装类型，例如 VO 中的 Long id。
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            // Long.TYPE 处理基本类型 long，避免两个类型表现不一致。
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
