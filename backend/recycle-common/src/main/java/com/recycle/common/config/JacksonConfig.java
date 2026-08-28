package com.recycle.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/**
 * Jackson 3（tools.jackson）序列化定制：
 * Long/BigDecimal 序列化为字符串，防止 JS 精度丢失、金额保留刻度。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule recycleSerializeModule() {
        SimpleModule module = new SimpleModule("recycle-serialize-module");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        return module;
    }
}
