package com.discordadmindashboard.exception;

/**
 * Thrown when a bot-backed operation is requested but the JDA connection is not
 * available (e.g. no bot token configured). Maps to HTTP 503.
 */
public class BotUnavailableException extends RuntimeException {
    public BotUnavailableException(String message) {
        super(message);
    }
}
