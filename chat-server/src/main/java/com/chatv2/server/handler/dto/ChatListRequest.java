package com.chatv2.server.handler.dto;

import java.util.UUID;

/**
 * DTO for chat list request payload.
 * JSON format: {"userId": "uuid"}
 */
public record ChatListRequest(
    UUID userId
) {
    /**
     * Validates the chat list request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }
}
