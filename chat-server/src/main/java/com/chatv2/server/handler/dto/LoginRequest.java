package com.chatv2.server.handler.dto;

/**
 * DTO for login request payload.
 * JSON format: {"username": "xxx", "password": "xxx"}
 */
public record LoginRequest(
    String username,
    String password
) {
    /**
     * Validates the login request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
    }
}
