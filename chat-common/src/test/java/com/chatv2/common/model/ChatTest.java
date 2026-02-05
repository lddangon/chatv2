package com.chatv2.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatTest {

    private static final UUID CHAT_ID = UUID.randomUUID();
    private static final String NAME = "Test Chat";
    private static final String DESCRIPTION = "Test chat description";
    private static final ChatType CHAT_TYPE = ChatType.GROUP;
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final String AVATAR_DATA = "avatarData";
    private static final int PARTICIPANT_COUNT = 3;
    private static final Instant CREATED_AT = Instant.now();
    private static final Instant UPDATED_AT = Instant.now();

    @Test
    @DisplayName("Should create Chat with all fields")
    void testChatCreation() {
        // When
        Chat chat = new Chat(
                CHAT_ID, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        // Then
        assertThat(chat.chatId()).isEqualTo(CHAT_ID);
        assertThat(chat.chatType()).isEqualTo(CHAT_TYPE);
        assertThat(chat.name()).isEqualTo(NAME);
        assertThat(chat.description()).isEqualTo(DESCRIPTION);
        assertThat(chat.ownerId()).isEqualTo(OWNER_ID);
        assertThat(chat.avatarData()).isEqualTo(AVATAR_DATA);
        assertThat(chat.createdAt()).isEqualTo(CREATED_AT);
        assertThat(chat.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(chat.participantCount()).isEqualTo(PARTICIPANT_COUNT);
    }

    @Test
    @DisplayName("Should create Chat with current time when createdAt is null")
    void testChatCreationWithNullCreatedAt() {
        // When
        Instant beforeCreation = Instant.now();
        Chat chat = new Chat(
                CHAT_ID, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, null, UPDATED_AT, PARTICIPANT_COUNT
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(chat.createdAt()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should create Chat with current time when updatedAt is null")
    void testChatCreationWithNullUpdatedAt() {
        // When
        Instant beforeCreation = Instant.now();
        Chat chat = new Chat(
                CHAT_ID, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, null, PARTICIPANT_COUNT
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(chat.updatedAt()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should set participant count to 0 when negative value is provided")
    void testChatCreationWithNegativeParticipantCount() {
        // When
        Chat chat = new Chat(
                CHAT_ID, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, -5
        );

        // Then
        assertThat(chat.participantCount()).isEqualTo(0);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when chatId is null")
    void testChatCreationWithNullChatId(UUID chatId) {
        // When/Then
        assertThatThrownBy(() -> new Chat(
                chatId, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chatId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when chatType is null")
    void testChatCreationWithNullChatType(ChatType chatType) {
        // When/Then
        assertThatThrownBy(() -> new Chat(
                CHAT_ID, chatType, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("chatType cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when group chat name is null or blank")
    void testGroupChatCreationWithInvalidName(String name) {
        // When/Then
        assertThatThrownBy(() -> new Chat(
                CHAT_ID, ChatType.GROUP, name, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group chat name cannot be null or blank");
    }

    @Test
    @DisplayName("Should create private chat with null name")
    void testPrivateChatCreationWithNullName() {
        // When
        Chat chat = new Chat(
                CHAT_ID, ChatType.PRIVATE, null, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        // Then
        assertThat(chat.name()).isNull();
    }

    @Test
    @DisplayName("Should create new group chat")
    void testCreateGroupChat() {
        // Given
        UUID owner = UUID.randomUUID();
        String groupName = "Test Group";
        String groupDescription = "Test group description";
        String groupAvatar = "groupAvatar";

        // When
        Chat chat = Chat.createGroupChat(owner, groupName, groupDescription, groupAvatar);

        // Then
        assertThat(chat.chatId()).isNotNull();
        assertThat(chat.chatType()).isEqualTo(ChatType.GROUP);
        assertThat(chat.name()).isEqualTo(groupName);
        assertThat(chat.description()).isEqualTo(groupDescription);
        assertThat(chat.ownerId()).isEqualTo(owner);
        assertThat(chat.avatarData()).isEqualTo(groupAvatar);
        assertThat(chat.createdAt()).isNotNull();
        assertThat(chat.updatedAt()).isNotNull();
        assertThat(chat.participantCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create new private chat")
    void testCreatePrivateChat() {
        // Given
        UUID owner = UUID.randomUUID();

        // When
        Chat chat = Chat.createPrivateChat(owner);

        // Then
        assertThat(chat.chatId()).isNotNull();
        assertThat(chat.chatType()).isEqualTo(ChatType.PRIVATE);
        assertThat(chat.name()).isNull();
        assertThat(chat.description()).isEqualTo("Private chat");
        assertThat(chat.ownerId()).isEqualTo(owner);
        assertThat(chat.avatarData()).isNull();
        assertThat(chat.createdAt()).isNotNull();
        assertThat(chat.updatedAt()).isNotNull();
        assertThat(chat.participantCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update chat info")
    void testWithUpdates() {
        // Given
        Chat originalChat = new Chat(
                CHAT_ID, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        String newName = "Updated Chat Name";
        String newDescription = "Updated chat description";
        String newAvatarData = "newAvatarData";

        // When
        Chat updatedChat = originalChat.withUpdates(newName, newDescription, newAvatarData);

        // Then
        assertThat(updatedChat.chatId()).isEqualTo(originalChat.chatId());
        assertThat(updatedChat.chatType()).isEqualTo(originalChat.chatType());
        assertThat(updatedChat.name()).isEqualTo(newName);
        assertThat(updatedChat.description()).isEqualTo(newDescription);
        assertThat(updatedChat.ownerId()).isEqualTo(originalChat.ownerId());
        assertThat(updatedChat.avatarData()).isEqualTo(newAvatarData);
        assertThat(updatedChat.createdAt()).isEqualTo(originalChat.createdAt());
        assertThat(updatedChat.updatedAt()).isAfter(originalChat.updatedAt());
        assertThat(updatedChat.participantCount()).isEqualTo(originalChat.participantCount());
    }

    @Test
    @DisplayName("Should not update name for private chat")
    void testPrivateChatWithUpdatesName() {
        // Given
        Chat originalPrivateChat = new Chat(
                CHAT_ID, ChatType.PRIVATE, null, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        String newName = "Updated Chat Name";
        String newDescription = "Updated chat description";
        String newAvatarData = "newAvatarData";

        // When
        Chat updatedChat = originalPrivateChat.withUpdates(newName, newDescription, newAvatarData);

        // Then
        assertThat(updatedChat.name()).isNull(); // Should remain null for private chat
    }

    @Test
    @DisplayName("Should update participant count")
    void testWithParticipantCount() {
        // Given
        Chat originalChat = new Chat(
                CHAT_ID, CHAT_TYPE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        int newCount = PARTICIPANT_COUNT + 2;

        // When
        Chat updatedChat = originalChat.withParticipantCount(newCount);

        // Then
        assertThat(updatedChat.chatId()).isEqualTo(originalChat.chatId());
        assertThat(updatedChat.chatType()).isEqualTo(originalChat.chatType());
        assertThat(updatedChat.name()).isEqualTo(originalChat.name());
        assertThat(updatedChat.description()).isEqualTo(originalChat.description());
        assertThat(updatedChat.ownerId()).isEqualTo(originalChat.ownerId());
        assertThat(updatedChat.avatarData()).isEqualTo(originalChat.avatarData());
        assertThat(updatedChat.createdAt()).isEqualTo(originalChat.createdAt());
        assertThat(updatedChat.updatedAt()).isAfter(originalChat.updatedAt());
        assertThat(updatedChat.participantCount()).isEqualTo(newCount);
    }

    @Test
    @DisplayName("Should check if chat is private")
    void testIsPrivate() {
        // Given
        Chat privateChat = new Chat(
                CHAT_ID, ChatType.PRIVATE, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        Chat groupChat = new Chat(
                CHAT_ID, ChatType.GROUP, NAME, DESCRIPTION, OWNER_ID, AVATAR_DATA, CREATED_AT, UPDATED_AT, PARTICIPANT_COUNT
        );

        // Then
        assertThat(privateChat.chatType()).isEqualTo(ChatType.PRIVATE);
        assertThat(groupChat.chatType()).isEqualTo(ChatType.GROUP);
    }
}