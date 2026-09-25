package com.discordadmindashboard.dto;

/**
 * A guild member surfaced to the dashboard. {@code globalName} carries Discord's
 * new (non-discriminator) display name when present; {@code discriminator} is
 * "0" for migrated accounts.
 */
public record MemberDto(
        String id,
        String username,
        String globalName,
        String discriminator,
        String avatar
) {
}
