package com.campus.trade.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 序列化的全局配置。
 *
 * <p>序列化是“Java 对象 → JSON”，反序列化是“JSON → Java 对象”。
 * 把规则集中在这里后，每个 VO 都不需要重复添加 Long 转字符串注解。</p>
 */
@Configuration
public class JacksonConfig {

    /** 全项目统一的时间格式，与前端展示、请求参数保持一致。 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

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

    /**
     * 将 LocalDateTime 统一按 {@code yyyy-MM-dd HH:mm:ss} 读写。
     *
     * <p>注意 application.yml 里的 {@code spring.jackson.date-format} 只对老的 {@code java.util.Date}
     * 生效，对 {@code LocalDateTime} 无效——没有这个 Bean 时，未加 {@code @JsonFormat} 的字段会输出成
     * {@code 2026-07-24T10:15:30} 这种带 T 的 ISO 格式，同一份接口里出现两种时间格式。
     * 这里统一注册序列化器和反序列化器，前后端只需要认一种格式。</p>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> {
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        };
    }
}
