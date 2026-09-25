package com.discordadmindashboard.dto;

/**
 * A selectable Discord permission.
 *
 * @param name  the enum name the API expects (e.g. {@code KICK_MEMBERS})
 * @param label a human-friendly label for the UI (e.g. "Kick Members")
 */
public record PermissionOption(String name, String label) {
}
