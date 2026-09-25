package com.discordadmindashboard.security;

import java.util.Optional;

/**
 * Request-scoped holder for the authenticated Discord user. Populated by
 * {@link JwtAuthenticationFilter} at the start of each request and cleared in a
 * {@code finally} block so the {@link ThreadLocal} never leaks across pooled threads.
 */
public final class UserContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static Optional<AuthenticatedUser> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
