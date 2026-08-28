package com.recycle.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

/**
 * Jackson 3（tools.jackson）序列化定制：
 * 包装型 Long（雪花 ID 等）/BigDecimal（金额）序列化为字符串，防止 JS 精度丢失、金额保留刻度；
 * 基本型 long（ts、total、pages 等小数值）保持数字。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule recycleSerializeModule() {
        SimpleModule module = new SimpleModule("recycle-serialize-module");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        return module;
    }
}
