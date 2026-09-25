package com.discordadmindashboard.exception;

/** Thrown when a guild, member or role cannot be found. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
