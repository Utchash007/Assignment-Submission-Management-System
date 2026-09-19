package com.asms.springasms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @DefaultValue("OnnoRokomBackend") String issuer,
        @DefaultValue("OnnoRokomFrontend") String audience,
        String signingKey,
        @DefaultValue("60") int accessTokenLifetimeMinutes
) {
}
