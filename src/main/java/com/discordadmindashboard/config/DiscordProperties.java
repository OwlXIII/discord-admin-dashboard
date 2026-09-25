package com.discordadmindashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Discord OAuth2 endpoints and application credentials, bound from the
 * {@code discord.*} properties. Using a record gives immutable, constructor-bound
 * configuration instead of the mutable getter/setter bean in the original project.
 */
@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizeUrl,
        String tokenUrl,
        String userInfoUrl,
        String guildsUrl,
        String botToken
) {
    public boolean hasBotToken() {
        return botToken != null && !botToken.isBlank();
    }

    public boolean hasOAuthCredentials() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
