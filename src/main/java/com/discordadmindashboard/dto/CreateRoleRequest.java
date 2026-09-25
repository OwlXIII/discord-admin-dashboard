package com.discordadmindashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating a new role. Discord role names are limited to 100 chars.
 */
public record CreateRoleRequest(
        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name must be 100 characters or fewer")
        String name,

        List<String> permissions
) {
    public List<String> permissionsOrEmpty() {
        return permissions == null ? List.of() : permissions;
    }
}
