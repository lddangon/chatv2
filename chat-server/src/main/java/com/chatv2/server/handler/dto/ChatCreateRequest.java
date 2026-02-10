package com.chatv2.server.handler.dto;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for chat creation request payload.
 * JSON format: {"chatType": "GROUP|PRIVATE", "name": "xxx", "description": "xxx", "memberIds": ["uuid", ...]}
 */
public record ChatCreateRequest(
    String chatType,
    String name,
    String description,
    UUID ownerId,
    Set<UUID> memberIds
) {
    /**
     * Validates the chat creation request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (chatType == null || chatType.isBlank()) {
            throw new IllegalArgumentException("chatType cannot be null or blank");
        }
        if ("GROUP".equals(chatType)) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Group chat name cannot be null or blank");
            }
            if (ownerId == null) {
                throw new IllegalArgumentException("ownerId cannot be null for group chat");
            }
        }
    }
}
