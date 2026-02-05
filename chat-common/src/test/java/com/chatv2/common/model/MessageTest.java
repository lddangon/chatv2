package com.chatv2.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTest {

    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID CHAT_ID = UUID.randomUUID();
    private static final UUID SENDER_ID = UUID.randomUUID();
    private static final String CONTENT = "Test message";
    private static final MessageType MESSAGE_TYPE = MessageType.TEXT;
    private static final UUID REPLY_TO_MESSAGE_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.now();
    private static final Instant EDITED_AT = Instant.now();
    private static final Instant DELETED_AT = Instant.now();
    private static final List<UUID> READ_BY = List.of(UUID.randomUUID(), UUID.randomUUID());

    @Test
    @DisplayName("Should create Message with all fields")
    void testMessageCreation() {
        // When
        Message message = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        );

        // Then
        assertThat(message.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(message.chatId()).isEqualTo(CHAT_ID);
        assertThat(message.senderId()).isEqualTo(SENDER_ID);
        assertThat(message.content()).isEqualTo(CONTENT);
        assertThat(message.messageType()).isEqualTo(MESSAGE_TYPE);
        assertThat(message.replyToMessageId()).isEqualTo(REPLY_TO_MESSAGE_ID);
        assertThat(message.createdAt()).isEqualTo(CREATED_AT);
        assertThat(message.editedAt()).isEqualTo(EDITED_AT);
        assertThat(message.deletedAt()).isEqualTo(DELETED_AT);
        assertThat(message.readBy()).isEqualTo(READ_BY);
    }

    @Test
    @DisplayName("Should create Message with default message type TEXT when null is provided")
    void testMessageCreationWithNullMessageType() {
        // When
        Message message = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, null, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        );

        // Then
        assertThat(message.messageType()).isEqualTo(MessageType.TEXT);
    }

    @Test
    @DisplayName("Should create Message with current time when createdAt is null")
    void testMessageCreationWithNullCreatedAt() {
        // When
        Instant beforeCreation = Instant.now();
        Message message = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                null, EDITED_AT, DELETED_AT, READ_BY
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(message.createdAt()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should create Message with empty list when readBy is null")
    void testMessageCreationWithNullReadBy() {
        // When
        Message message = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, null
        );

        // Then
        assertThat(message.readBy()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when messageId is null")
    void testMessageCreationWithNullMessageId(UUID messageId) {
        // When/Then
        assertThatThrownBy(() -> new Message(
                messageId, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("messageId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when chatId is null")
    void testMessageCreationWithNullChatId(UUID chatId) {
        // When/Then
        assertThatThrownBy(() -> new Message(
                MESSAGE_ID, chatId, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chatId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when senderId is null")
    void testMessageCreationWithNullSenderId(UUID senderId) {
        // When/Then
        assertThatThrownBy(() -> new Message(
                MESSAGE_ID, CHAT_ID, senderId, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("senderId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when content is null or blank")
    void testMessageCreationWithInvalidContent(String content) {
        // When/Then
        assertThatThrownBy(() -> new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, content, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("content cannot be null or blank");
    }

    @Test
    @DisplayName("Should create new message with random UUID")
    void testCreateNewMessage() {
        // When
        Message message = Message.createNew(CHAT_ID, SENDER_ID, CONTENT, MessageType.IMAGE);

        // Then
        assertThat(message.messageId()).isNotNull();
        assertThat(message.chatId()).isEqualTo(CHAT_ID);
        assertThat(message.senderId()).isEqualTo(SENDER_ID);
        assertThat(message.content()).isEqualTo(CONTENT);
        assertThat(message.messageType()).isEqualTo(MessageType.IMAGE);
        assertThat(message.replyToMessageId()).isNull();
        assertThat(message.createdAt()).isNotNull();
        assertThat(message.editedAt()).isNull();
        assertThat(message.deletedAt()).isNull();
        assertThat(message.readBy()).isEmpty();
    }

    @Test
    @DisplayName("Should create reply message")
    void testCreateReplyMessage() {
        // When
        Message message = Message.createReply(CHAT_ID, SENDER_ID, CONTENT, MessageType.TEXT, REPLY_TO_MESSAGE_ID);

        // Then
        assertThat(message.messageId()).isNotNull();
        assertThat(message.chatId()).isEqualTo(CHAT_ID);
        assertThat(message.senderId()).isEqualTo(SENDER_ID);
        assertThat(message.content()).isEqualTo(CONTENT);
        assertThat(message.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(message.replyToMessageId()).isEqualTo(REPLY_TO_MESSAGE_ID);
        assertThat(message.createdAt()).isNotNull();
        assertThat(message.editedAt()).isNull();
        assertThat(message.deletedAt()).isNull();
        assertThat(message.readBy()).isEmpty();
    }

    @Test
    @DisplayName("Should create edited version of message")
    void testWithEditedContent() {
        // Given
        Message originalMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, null, DELETED_AT, READ_BY
        );
        String newContent = "Updated message content";

        // When
        Message editedMessage = originalMessage.withEditedContent(newContent);

        // Then
        assertThat(editedMessage.messageId()).isEqualTo(originalMessage.messageId());
        assertThat(editedMessage.chatId()).isEqualTo(originalMessage.chatId());
        assertThat(editedMessage.senderId()).isEqualTo(originalMessage.senderId());
        assertThat(editedMessage.content()).isEqualTo(newContent);
        assertThat(editedMessage.messageType()).isEqualTo(originalMessage.messageType());
        assertThat(editedMessage.replyToMessageId()).isEqualTo(originalMessage.replyToMessageId());
        assertThat(editedMessage.createdAt()).isEqualTo(originalMessage.createdAt());
        assertThat(editedMessage.editedAt()).isNotNull();
        assertThat(editedMessage.deletedAt()).isEqualTo(originalMessage.deletedAt());
        assertThat(editedMessage.readBy()).isEqualTo(originalMessage.readBy());
    }

    @Test
    @DisplayName("Should create deleted version of message")
    void testAsDeleted() {
        // Given
        Message originalMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, null, READ_BY
        );

        // When
        Message deletedMessage = originalMessage.asDeleted();

        // Then
        assertThat(deletedMessage.messageId()).isEqualTo(originalMessage.messageId());
        assertThat(deletedMessage.chatId()).isEqualTo(originalMessage.chatId());
        assertThat(deletedMessage.senderId()).isEqualTo(originalMessage.senderId());
        assertThat(deletedMessage.content()).isEqualTo("[Message deleted]");
        assertThat(deletedMessage.messageType()).isEqualTo(originalMessage.messageType());
        assertThat(deletedMessage.replyToMessageId()).isEqualTo(originalMessage.replyToMessageId());
        assertThat(deletedMessage.createdAt()).isEqualTo(originalMessage.createdAt());
        assertThat(deletedMessage.editedAt()).isEqualTo(originalMessage.editedAt());
        assertThat(deletedMessage.deletedAt()).isNotNull();
        assertThat(deletedMessage.readBy()).isEqualTo(originalMessage.readBy());
    }

    @Test
    @DisplayName("Should mark message as read by user")
    void testMarkAsRead() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        Message originalMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, List.of(existingUserId)
        );

        // When
        Message markedMessage = originalMessage.markAsRead(userId);

        // Then
        assertThat(markedMessage.readBy()).contains(userId);
        assertThat(markedMessage.readBy()).contains(existingUserId);
        assertThat(markedMessage.readBy()).hasSize(originalMessage.readBy().size() + 1);
    }

    @Test
    @DisplayName("Should not duplicate user in readBy list")
    void testMarkAsReadWhenAlreadyRead() {
        // Given
        UUID userId = UUID.randomUUID();
        Message originalMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, List.of(userId)
        );

        // When
        Message markedMessage = originalMessage.markAsRead(userId);

        // Then
        assertThat(markedMessage.readBy()).containsExactly(userId);
    }

    @Test
    @DisplayName("Should return true if message is deleted")
    void testIsDeleted() {
        // Given
        Message deletedMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        );

        // Then
        assertThat(deletedMessage.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should return false if message is not deleted")
    void testIsNotDeleted() {
        // Given
        Message nonDeletedMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, null, READ_BY
        );

        // Then
        assertThat(nonDeletedMessage.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should return true if message is edited")
    void testIsEdited() {
        // Given
        Message editedMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        );

        // Then
        assertThat(editedMessage.isEdited()).isTrue();
    }

    @Test
    @DisplayName("Should return false if message is not edited")
    void testIsNotEdited() {
        // Given
        Message nonEditedMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, null, DELETED_AT, READ_BY
        );

        // Then
        assertThat(nonEditedMessage.isEdited()).isFalse();
    }

    @Test
    @DisplayName("Should return true if message is a reply")
    void testIsReply() {
        // Given
        Message replyMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, REPLY_TO_MESSAGE_ID,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        );

        // Then
        assertThat(replyMessage.isReply()).isTrue();
    }

    @Test
    @DisplayName("Should return false if message is not a reply")
    void testIsNotReply() {
        // Given
        Message nonReplyMessage = new Message(
                MESSAGE_ID, CHAT_ID, SENDER_ID, CONTENT, MESSAGE_TYPE, null,
                CREATED_AT, EDITED_AT, DELETED_AT, READ_BY
        );

        // Then
        assertThat(nonReplyMessage.isReply()).isFalse();
    }
}