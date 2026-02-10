package com.chatv2.server.handler.dto;

/**
 * DTO for session validation request payload.
 * JSON format: {"token": "xxx"}
 */
public record SessionValidateRequest(
    String token
) {
    /**
     * Validates the session validation request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token cannot be null or blank");
        }
    }
}
