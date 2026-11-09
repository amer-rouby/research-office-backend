package com.researchoffice.shared.web;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI document is the single source of the web client's types.
 * Convention: every DTO property is required unless marked {@code @Schema(nullable = true)}.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenApiCustomizer requiredByDefault(@Value("${spring.application.name}") String name,
                                        @Value("${info.app.version:dev}") String version) {
        return openApi -> {
            openApi.info(new Info().title(name).version(version));
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            openApi.getComponents().getSchemas().values().forEach(OpenApiConfig::requireNonNullable);
        };
    }

    @SuppressWarnings("rawtypes")
    private static void requireNonNullable(Schema schema) {
        Map<String, Schema> props = schema.getProperties();
        if (props == null || props.isEmpty()) {
            return;
        }
        var required = new ArrayList<String>();
        props.forEach((key, prop) -> {
            boolean nullable = Boolean.TRUE.equals(prop.getNullable())
                    || (prop.getTypes() != null && prop.getTypes().contains("null"));
            if (!nullable) {
                required.add(key);
            }
        });
        schema.setRequired(required);
    }
}
