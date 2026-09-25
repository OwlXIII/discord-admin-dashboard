package com.discordadmindashboard.exception;

/** Thrown when the request has no valid Discord session. Maps to HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
