package com.discordadmindashboard.security;

import com.discordadmindashboard.config.AppProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService serviceWithSecret(String secret) {
        AppProperties props = new AppProperties(
                "http://localhost:8080/dashboard.html",
                false,
                new AppProperties.Jwt(secret, 60)
        );
        return new JwtService(props);
    }

    @Test
    void generatesAndValidatesRoundTrip() {
        JwtService service = serviceWithSecret("this-is-a-sufficiently-long-test-secret-123456");

        String token = service.generateToken(Map.of(
                "id", "42",
                "username", "tester",
                "discord_token", "abc"
        ));

        Claims claims = service.validateToken(token).getPayload();
        assertEquals("42", claims.get("id", String.class));
        assertEquals("tester", claims.get("username", String.class));
        assertEquals("abc", claims.get("discord_token", String.class));
    }

    @Test
    void tokenFromDifferentKeyIsRejected() {
        JwtService issuer = serviceWithSecret("this-is-a-sufficiently-long-test-secret-123456");
        JwtService other = serviceWithSecret("a-completely-different-secret-of-good-length-000");

        String token = issuer.generateToken(Map.of("id", "1"));

        assertThrows(io.jsonwebtoken.JwtException.class, () -> other.validateToken(token));
    }
}
