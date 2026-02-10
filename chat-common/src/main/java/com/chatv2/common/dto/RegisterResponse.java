package com.chatv2.common.dto;

import java.util.UUID;

/**
 * Register response DTO.
 * Contains success status, user ID, and message.
 */
public record RegisterResponse(
    boolean success,
    String message,
    UUID userId
) {
    public static RegisterResponse success(UUID userId) {
        return new RegisterResponse(true, "Registration successful", userId);
    }

    public static RegisterResponse failure(String message) {
        return new RegisterResponse(false, message, null);
    }
}
