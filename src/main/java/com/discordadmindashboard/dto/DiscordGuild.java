package com.discordadmindashboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A guild (server) as returned by Discord's {@code /users/@me/guilds} endpoint.
 * Only the fields we use are mapped; unknown ones are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordGuild(
        String id,
        String name,
        String icon,
        boolean owner,
        long permissions
) {
    /** Discord's ADMINISTRATOR permission bit. */
    private static final long ADMINISTRATOR = 0x8L;

    /** True if the user is the owner or holds the ADMINISTRATOR permission on this guild. */
    public boolean isAdmin() {
        return owner || (permissions & ADMINISTRATOR) == ADMINISTRATOR;
    }
}
