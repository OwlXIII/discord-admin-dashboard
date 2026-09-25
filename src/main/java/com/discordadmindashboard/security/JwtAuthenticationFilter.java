package com.discordadmindashboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code jwt} session cookie, validates it and exposes the resolved
 * Discord identity through {@link UserContext} for the duration of the request.
 * Extends {@link OncePerRequestFilter} so it runs exactly once per request even
 * with async dispatches (the original implemented raw {@code Filter}).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "jwt";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        try {
            resolveCookie(request).ifPresent(cookie -> {
                try {
                    Claims claims = jwtService.validateToken(cookie.getValue()).getPayload();
                    UserContext.set(new AuthenticatedUser(
                            claims.get("id", String.class),
                            claims.get("username", String.class),
                            claims.get("discord_token", String.class)
                    ));
                } catch (JwtException e) {
                    log.debug("Rejected invalid JWT cookie: {}", e.getMessage());
                }
            });
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private java.util.Optional<Cookie> resolveCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return java.util.Optional.of(cookie);
            }
        }
        return java.util.Optional.empty();
    }
}
