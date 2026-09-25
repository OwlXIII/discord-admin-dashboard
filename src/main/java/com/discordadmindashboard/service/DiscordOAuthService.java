package com.discordadmindashboard.service;

import com.discordadmindashboard.config.DiscordProperties;
import com.discordadmindashboard.dto.DiscordGuild;
import com.discordadmindashboard.dto.DiscordUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Talks to the Discord HTTP API on behalf of the logged-in user (OAuth2 access
 * token): token exchange, profile lookup and the user's administrable guilds.
 */
@Service
public class DiscordOAuthService {

    private static final Logger log = LoggerFactory.getLogger(DiscordOAuthService.class);

    private final DiscordProperties config;
    private final RestClient restClient;

    public DiscordOAuthService(DiscordProperties config) {
        this.config = config;
        this.restClient = RestClient.create();
    }

    /** Exchanges an OAuth2 authorization code for the user's access token. */
    public String exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", config.clientId());
        form.add("client_secret", config.clientSecret());
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", config.redirectUri());

        Map<?, ?> body = restClient.post()
                .uri(config.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        Object token = body == null ? null : body.get("access_token");
        if (token == null) {
            throw new IllegalStateException("Discord did not return an access token.");
        }
        return token.toString();
    }

    /** Fetches the profile of the user who owns the given access token. */
    public DiscordUser getCurrentUser(String accessToken) {
        return restClient.get()
                .uri(config.userInfoUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(DiscordUser.class);
    }

    /** Returns only the guilds the user can administer (owner or ADMINISTRATOR). */
    public List<DiscordGuild> getAdministrableGuilds(String accessToken) {
        DiscordGuild[] guilds = restClient.get()
                .uri(config.guildsUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(DiscordGuild[].class);

        if (guilds == null) {
            return List.of();
        }
        List<DiscordGuild> admin = Arrays.stream(guilds)
                .filter(DiscordGuild::isAdmin)
                .toList();
        log.debug("User can administer {} of {} guilds", admin.size(), guilds.length);
        return admin;
    }
}
