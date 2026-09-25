package com.discordadmindashboard.controller;

import com.discordadmindashboard.dto.ApiMessage;
import com.discordadmindashboard.dto.DiscordGuild;
import com.discordadmindashboard.exception.UnauthorizedException;
import com.discordadmindashboard.security.AuthenticatedUser;
import com.discordadmindashboard.security.JwtAuthenticationFilter;
import com.discordadmindashboard.security.UserContext;
import com.discordadmindashboard.service.DiscordBotService;
import com.discordadmindashboard.service.DiscordOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ServerController {

    private final DiscordOAuthService oauthService;
    private final DiscordBotService botService;

    public ServerController(DiscordOAuthService oauthService, DiscordBotService botService) {
        this.oauthService = oauthService;
        this.botService = botService;
    }

    /** The logged-in user's basic profile (used by the dashboard header). */
    @GetMapping("/me")
    public Map<String, String> me() {
        AuthenticatedUser user = currentUser();
        return Map.of("id", user.id(), "username", user.username());
    }

    /**
     * Guilds the logged-in user can administer, narrowed to those the bot is also
     * in (only those are actually manageable). If the bot is offline, the full
     * admin list is returned so the UI still shows something.
     */
    @GetMapping("/servers")
    public List<DiscordGuild> servers() {
        List<DiscordGuild> adminGuilds =
                oauthService.getAdministrableGuilds(currentUser().discordToken());
        if (!botService.isAvailable()) {
            return adminGuilds;
        }
        Set<String> botGuildIds = botService.getBotGuildIds();
        return adminGuilds.stream()
                .filter(guild -> botGuildIds.contains(guild.id()))
                .toList();
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessage> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(new ApiMessage("Logged out."));
    }

    private AuthenticatedUser currentUser() {
        return UserContext.get()
                .orElseThrow(() -> new UnauthorizedException("Not logged in."));
    }
}
