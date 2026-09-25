package com.discordadmindashboard.security;

import com.discordadmindashboard.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtService(AppProperties appProperties) {
        AppProperties.Jwt cfg = appProperties.jwt();
        this.ttl = Duration.ofMinutes(cfg != null && cfg.ttlMinutes() > 0 ? cfg.ttlMinutes() : 60);

        String secret = cfg != null ? cfg.secret() : null;
        if (secret != null && !secret.isBlank()) {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        } else {
            // No configured secret: generate an ephemeral key. Fine for local dev,
            // but sessions won't survive a restart — warn so it isn't used in prod.
            this.signingKey = Jwts.SIG.HS256.key().build();
            log.warn("app.jwt.secret is not set; using an ephemeral signing key. "
                    + "Set APP_JWT_SECRET to keep sessions valid across restarts.");
        }
    }

    public String generateToken(Map<String, ?> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    public Jws<Claims> validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    public Duration ttl() {
        return ttl;
    }
}
