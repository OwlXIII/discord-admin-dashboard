package com.discordadmindashboard.security;

/**
 * The Discord identity resolved from the JWT session cookie for the current request.
 *
 * @param id           Discord user id (snowflake)
 * @param username     Discord username
 * @param discordToken the user's OAuth2 access token, used to call Discord as the user
 */
public record AuthenticatedUser(String id, String username, String discordToken) {
}
