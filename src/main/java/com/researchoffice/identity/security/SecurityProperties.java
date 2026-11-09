package com.researchoffice.identity.security;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ro.security")
record SecurityProperties(String jwtSecret, List<String> corsOrigins) {

    SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ro.security.jwt-secret (RO_JWT_SECRET) must be at least 32 bytes");
        }
        corsOrigins = corsOrigins == null ? List.of() : List.copyOf(corsOrigins);
    }
}
