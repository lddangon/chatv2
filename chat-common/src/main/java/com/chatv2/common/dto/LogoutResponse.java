package com.chatv2.common.dto;

/**
 * Logout response DTO.
 * Contains success status and message.
 */
public record LogoutResponse(
    boolean success,
    String message
) {
    public static LogoutResponse createSuccess() {
        return new LogoutResponse(true, "Logout successful");
    }

    public static LogoutResponse failure(String message) {
        return new LogoutResponse(false, message);
    }
}
