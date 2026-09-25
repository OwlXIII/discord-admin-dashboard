package com.discordadmindashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level settings bound from {@code app.*}.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String dashboardUrl,
        boolean cookieSecure,
        Jwt jwt
) {
    public record Jwt(
            String secret,
            long ttlMinutes
    ) {}
}
