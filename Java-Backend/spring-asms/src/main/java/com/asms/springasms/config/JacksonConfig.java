package com.asms.springasms.config;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Matches .NET's timestamp wire format: ISO-8601 UTC with trailing zeros trimmed
 * from the fractional part ({@code 2026-09-24T18:01:30.99273Z}, not
 * {@code ...30.992730Z}). PostgreSQL timestamptz stores microseconds, so at most
 * 6 fractional digits are ever emitted. Jackson's default ISO-8601 output uses
 * fixed 0/3/6/9-digit buckets instead.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME_PART =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC);

    static String formatInstant(Instant instant) {
        int micros = instant.getNano() / 1_000;
        String base = DATE_TIME_PART.format(instant);
        if (micros == 0) {
            return base + "Z";
        }
        String fraction = String.format("%06d", micros).replaceAll("0+$", "");
        return base + "." + fraction + "Z";
    }

    @Bean
    JsonMapperBuilderCustomizer instantSerializerCustomizer() {
        return builder -> builder.addModule(new SimpleModule().addSerializer(Instant.class,
                new ValueSerializer<Instant>() {
                    @Override
                    public void serialize(Instant value, JsonGenerator gen,
                                          SerializationContext serializers) {
                        gen.writeString(formatInstant(value));
                    }
                }));
    }
}
