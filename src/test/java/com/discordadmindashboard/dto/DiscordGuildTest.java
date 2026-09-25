package com.discordadmindashboard.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordGuildTest {

    private static final long ADMINISTRATOR = 0x8L;

    @Test
    void ownerIsAlwaysAdmin() {
        DiscordGuild guild = new DiscordGuild("1", "Owned", null, true, 0L);
        assertTrue(guild.isAdmin());
    }

    @Test
    void administratorPermissionBitGrantsAdmin() {
        DiscordGuild guild = new DiscordGuild("2", "Managed", null, false, ADMINISTRATOR);
        assertTrue(guild.isAdmin());
    }

    @Test
    void administratorBitDetectedAmongOtherPermissions() {
        DiscordGuild guild = new DiscordGuild("3", "Mixed", null, false, ADMINISTRATOR | 0x400L);
        assertTrue(guild.isAdmin());
    }

    @Test
    void plainMemberIsNotAdmin() {
        DiscordGuild guild = new DiscordGuild("4", "Regular", null, false, 0x400L);
        assertFalse(guild.isAdmin());
    }
}
