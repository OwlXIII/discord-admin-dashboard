package com.discordadmindashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The authenticated user as returned by Discord's {@code /users/@me} endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordUser(
        String id,
        String username,
        String globalName,
        String avatar
) {
}
