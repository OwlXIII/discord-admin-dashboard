package com.discordadmindashboard.controller;

import com.discordadmindashboard.config.AppProperties;
import com.discordadmindashboard.config.DiscordProperties;
import com.discordadmindashboard.dto.DiscordUser;
import com.discordadmindashboard.security.JwtAuthenticationFilter;
import com.discordadmindashboard.security.JwtService;
import com.discordadmindashboard.service.DiscordOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handles the Discord OAuth2 authorization-code flow and issues the JWT session cookie.
 */
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);

    private final DiscordProperties discord;
    private final AppProperties app;
    private final DiscordOAuthService oauthService;
    private final JwtService jwtService;

    public OAuthController(DiscordProperties discord,
                          AppProperties app,
                          DiscordOAuthService oauthService,
                          JwtService jwtService) {
        this.discord = discord;
        this.app = app;
        this.oauthService = oauthService;
        this.jwtService = jwtService;
    }

    /** Step 1: redirect the browser to Discord's consent screen. */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        if (!discord.hasOAuthCredentials()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Discord OAuth is not configured. Set DISCORD_CLIENT_ID/DISCORD_CLIENT_SECRET.");
            return;
        }
        String url = discord.authorizeUrl()
                + "?client_id=" + URLEncoder.encode(discord.clientId(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(discord.redirectUri(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("identify guilds", StandardCharsets.UTF_8);
        response.sendRedirect(url);
    }

    /** Step 2: exchange the code, look up the user, set the cookie, land on the dashboard. */
    @GetMapping("/redirect")
    public void handleRedirect(@RequestParam("code") String code,
                               HttpServletResponse response) throws IOException {
        String accessToken = oauthService.exchangeCodeForToken(code);
        DiscordUser user = oauthService.getCurrentUser(accessToken);

        String jwt = jwtService.generateToken(Map.of(
                "id", user.id(),
                "username", user.username(),
                "discord_token", accessToken
        ));

        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, jwt)
                .httpOnly(true)
                .secure(app.cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.ttl())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("Issued session for Discord user {} ({})", user.username(), user.id());
        response.sendRedirect(app.dashboardUrl());
    }
}
