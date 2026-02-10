package com.chatv2.common.dto;

import java.util.UUID;

/**
 * Login response DTO.
 * Contains success status, user ID, and session token.
 */
public record LoginResponse(
    boolean success,
    String message,
    UUID userId,
    String token
) {
    public static LoginResponse success(UUID userId, String token) {
        return new LoginResponse(true, "Login successful", userId, token);
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse(false, message, null, null);
    }
}
