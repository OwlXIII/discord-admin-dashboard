package com.discordadmindashboard.controller;

import com.discordadmindashboard.dto.ApiMessage;
import com.discordadmindashboard.dto.CreateRoleRequest;
import com.discordadmindashboard.dto.MemberDto;
import com.discordadmindashboard.dto.RoleDto;
import com.discordadmindashboard.exception.UnauthorizedException;
import com.discordadmindashboard.security.UserContext;
import com.discordadmindashboard.service.DiscordBotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bot-backed member and role management for a guild. All endpoints require a
 * valid session; the actual Discord permission checks are enforced by Discord
 * itself when the bot performs each action.
 */
@RestController
@RequestMapping("/api/servers/{serverId}")
public class MemberController {

    private final DiscordBotService botService;

    public MemberController(DiscordBotService botService) {
        this.botService = botService;
    }

    @GetMapping("/users")
    public List<MemberDto> getUsers(@PathVariable("serverId") String serverId) {
        requireLogin();
        return botService.getMembers(serverId);
    }

    @GetMapping("/users/{userId}/roles")
    public List<RoleDto> getUserRoles(@PathVariable("serverId") String serverId, @PathVariable("userId") String userId) {
        requireLogin();
        return botService.getUserRoles(serverId, userId);
    }

    @GetMapping("/roles")
    public List<RoleDto> getRoles(@PathVariable("serverId") String serverId) {
        requireLogin();
        return botService.getRoles(serverId);
    }

    @PostMapping("/roles")
    public RoleDto createRole(@PathVariable("serverId") String serverId,
                              @Valid @RequestBody CreateRoleRequest request) {
        requireLogin();
        return botService.createRole(serverId, request.name(), request.permissionsOrEmpty());
    }

    @DeleteMapping("/users/{userId}")
    public ApiMessage kickUser(@PathVariable("serverId") String serverId, @PathVariable("userId") String userId) {
        requireLogin();
        botService.kickMember(serverId, userId);
        return new ApiMessage("User kicked.");
    }

    @PatchMapping("/users/{userId}/roles/{roleId}")
    public ApiMessage addRole(@PathVariable("serverId") String serverId,
                              @PathVariable("userId") String userId,
                              @PathVariable("roleId") String roleId) {
        requireLogin();
        botService.addRole(serverId, userId, roleId);
        return new ApiMessage("Role added to user.");
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ApiMessage removeRole(@PathVariable("serverId") String serverId,
                                 @PathVariable("userId") String userId,
                                 @PathVariable("roleId") String roleId) {
        requireLogin();
        botService.removeRole(serverId, userId, roleId);
        return new ApiMessage("Role removed from user.");
    }

    private void requireLogin() {
        if (UserContext.get().isEmpty()) {
            throw new UnauthorizedException("Not logged in.");
        }
    }
}
