package com.chatv2.server.handler.dto;

/**
 * DTO for logout request payload.
 * JSON format: {"token": "xxx"}
 */
public record LogoutRequest(
    String token
) {
    /**
     * Validates the logout request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
    }
}
