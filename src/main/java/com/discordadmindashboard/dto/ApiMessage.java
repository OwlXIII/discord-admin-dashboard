package com.discordadmindashboard.dto;

/**
 * Simple JSON envelope for status responses, e.g. {@code {"message": "User kicked."}}.
 */
public record ApiMessage(String message) {
}
