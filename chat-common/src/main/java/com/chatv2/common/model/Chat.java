package com.chatv2.common.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Chat metadata record representing a chat room or private chat.
 * Immutable data class following Java 21 record pattern.
 */
public record Chat(
    UUID chatId,
    ChatType chatType,
    String name,
    String description,
    UUID ownerId,
    String avatarData,
    Instant createdAt,
    Instant updatedAt,
    int participantCount
) {
    /**
     * Creates a new Chat with current timestamp.
     */
    public Chat {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId cannot be null");
        }
        if (chatType == null) {
            throw new IllegalArgumentException("chatType cannot be null");
        }
        if (chatType == ChatType.GROUP && (name == null || name.isBlank())) {
            throw new IllegalArgumentException("Group chat name cannot be null or blank");
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (participantCount < 0) {
            participantCount = 0;
        }
    }

    /**
     * Creates a new group chat.
     */
    public static Chat createGroupChat(UUID ownerId, String name, String description, String avatarData) {
        return new Chat(
            UUID.randomUUID(),
            ChatType.GROUP,
            name,
            description,
            ownerId,
            avatarData,
            Instant.now(),
            Instant.now(),
            1
        );
    }

    /**
     * Creates a new private chat.
     */
    public static Chat createPrivateChat(UUID ownerId) {
        return new Chat(
            UUID.randomUUID(),
            ChatType.PRIVATE,
            null,
            "Private chat",
            ownerId,
            null,
            Instant.now(),
            Instant.now(),
            2
        );
    }

    /**
     * Updates chat information.
     */
    public Chat withUpdates(String name, String description, String avatarData) {
        return new Chat(
            this.chatId,
            this.chatType,
            chatType == ChatType.GROUP && name != null ? name : this.name,
            description != null ? description : this.description,
            this.ownerId,
            avatarData != null ? avatarData : this.avatarData,
            this.createdAt,
            Instant.now(),
            this.participantCount
        );
    }

    /**
     * Updates participant count.
     */
    public Chat withParticipantCount(int count) {
        return new Chat(
            this.chatId,
            this.chatType,
            this.name,
            this.description,
            this.ownerId,
            this.avatarData,
            this.createdAt,
            Instant.now(),
            count
        );
    }
}
