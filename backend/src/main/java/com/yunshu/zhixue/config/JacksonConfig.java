package com.yunshu.zhixue.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 时间字段序列化约定（04 §2.11、D17）：
 * 数据库与内存统一 UTC，接口输出转 {@code Asia/Shanghai}（{@code yyyy-MM-dd HH:mm:ss}），
 * 入参按上海时间解析后转回 UTC。
 */
@Configuration
public class JacksonConfig {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset STORAGE_OFFSET = ZoneOffset.UTC;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("utc-local-datetime");
            module.addSerializer(LocalDateTime.class, new UtcLocalDateTimeSerializer());
            module.addDeserializer(LocalDateTime.class, new ShanghaiLocalDateTimeDeserializer());
            builder.modulesToInstall(module);
        };
    }

    /** UTC LocalDateTime → 上海时间字符串。 */
    private static final class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(value.toInstant(STORAGE_OFFSET).atZone(DISPLAY_ZONE).format(FORMATTER));
        }
    }

    /** 上海时间字符串 → UTC LocalDateTime。 */
    private static final class ShanghaiLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            LocalDateTime shanghaiTime;
            try {
                shanghaiTime = LocalDateTime.parse(text, FORMATTER);
            } catch (DateTimeParseException ex) {
                shanghaiTime = LocalDateTime.parse(text);
            }
            return shanghaiTime.atZone(DISPLAY_ZONE)
                    .withZoneSameInstant(STORAGE_OFFSET)
                    .toLocalDateTime();
        }
    }
}
