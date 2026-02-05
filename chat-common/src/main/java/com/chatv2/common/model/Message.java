package com.chatv2.common.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message record representing a chat message.
 * Immutable data class following Java 21 record pattern.
 */
public record Message(
    UUID messageId,
    UUID chatId,
    UUID senderId,
    String content,
    MessageType messageType,
    UUID replyToMessageId,
    Instant createdAt,
    Instant editedAt,
    Instant deletedAt,
    List<UUID> readBy
) {
    /**
     * Creates a new Message with current timestamp.
     */
    public Message {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId cannot be null");
        }
        if (chatId == null) {
            throw new IllegalArgumentException("chatId cannot be null");
        }
        if (senderId == null) {
            throw new IllegalArgumentException("senderId cannot be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (readBy == null) {
            readBy = List.of();
        }
    }

    /**
     * Creates a new message.
     */
    public static Message createNew(UUID chatId, UUID senderId, String content, MessageType messageType) {
        return new Message(
            UUID.randomUUID(),
            chatId,
            senderId,
            content,
            messageType,
            null,
            Instant.now(),
            null,
            null,
            List.of()
        );
    }

    /**
     * Creates a reply message.
     */
    public static Message createReply(
        UUID chatId,
        UUID senderId,
        String content,
        MessageType messageType,
        UUID replyToMessageId
    ) {
        return new Message(
            UUID.randomUUID(),
            chatId,
            senderId,
            content,
            messageType,
            replyToMessageId,
            Instant.now(),
            null,
            null,
            List.of()
        );
    }

    /**
     * Returns an edited version of the message.
     */
    public Message withEditedContent(String newContent) {
        return new Message(
            this.messageId,
            this.chatId,
            this.senderId,
            newContent,
            this.messageType,
            this.replyToMessageId,
            this.createdAt,
            Instant.now(),
            this.deletedAt,
            this.readBy
        );
    }

    /**
     * Returns a deleted version of the message.
     */
    public Message asDeleted() {
        return new Message(
            this.messageId,
            this.chatId,
            this.senderId,
            "[Message deleted]",
            this.messageType,
            this.replyToMessageId,
            this.createdAt,
            this.editedAt,
            Instant.now(),
            this.readBy
        );
    }

    /**
     * Marks message as read by adding the user ID to the readBy list.
     * Returns the same instance if the user has already read the message,
     * otherwise returns a new Message instance with the updated readBy list.
     *
     * @param userId the ID of the user who read the message
     * @return this Message if user already read it, or a new Message with updated readBy list
     * @throws IllegalArgumentException if userId is null
     */
    public Message markAsRead(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        // Return current instance if user already read the message
        if (this.readBy.contains(userId)) {
            return this;
        }

        // Create new instance with updated readBy list using Stream API
        List<UUID> updatedReadBy = List.copyOf(
            java.util.stream.Stream.concat(this.readBy.stream(), java.util.stream.Stream.of(userId))
                .toList()
        );

        return new Message(
            this.messageId,
            this.chatId,
            this.senderId,
            this.content,
            this.messageType,
            this.replyToMessageId,
            this.createdAt,
            this.editedAt,
            this.deletedAt,
            updatedReadBy
        );
    }

    /**
     * Checks if message is deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Checks if message is edited.
     */
    public boolean isEdited() {
        return editedAt != null;
    }

    /**
     * Checks if message is a reply.
     */
    public boolean isReply() {
        return replyToMessageId != null;
    }
}
