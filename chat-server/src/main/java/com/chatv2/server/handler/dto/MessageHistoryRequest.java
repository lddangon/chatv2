package com.chatv2.server.handler.dto;

import java.util.UUID;

/**
 * DTO for message history request payload.
 * JSON format: {"chatId": "uuid", "limit": N}
 */
public record MessageHistoryRequest(
    UUID chatId,
    int limit
) {
    /**
     * Validates the message history request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId cannot be null");
        }
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit must be between 1 and 1000");
        }
    }
}
