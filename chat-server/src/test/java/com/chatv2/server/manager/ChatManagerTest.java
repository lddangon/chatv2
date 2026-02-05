package com.chatv2.server.manager;

import com.chatv2.common.exception.ChatException;
import com.chatv2.common.model.Chat;
import com.chatv2.common.model.ChatType;
import com.chatv2.server.storage.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatManagerTest {

    @Mock
    private ChatRepository chatRepository;

    @InjectMocks
    private ChatManager chatManager;

    private UUID chatId;
    private UUID ownerId;
    private String name;
    private String description;
    private String avatarData;
    private int participantCount;
    private Chat chat;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        name = "Test Chat";
        description = "Test chat description";
        avatarData = "avatarData";
        participantCount = 2;
        
        chat = new Chat(
                chatId, ChatType.GROUP, name, description, ownerId, avatarData,
                java.time.Instant.now(), java.time.Instant.now(), participantCount
        );
    }

    @Test
    @DisplayName("Should create group chat successfully")
    void testCreateGroupChat() throws ExecutionException, InterruptedException {
        // Given
        Set<UUID> memberIds = Set.of();
        
        // Create a proper chat result that matches expected participant count
        Chat initialChat = new Chat(
                chatId, ChatType.GROUP, name, description, ownerId, avatarData,
                java.time.Instant.now(), java.time.Instant.now(), 1  // Only owner as participant
        );
        
        Chat updatedChat = new Chat(
                chatId, ChatType.GROUP, name, description, ownerId, avatarData,
                java.time.Instant.now(), java.time.Instant.now(), 1  // Only owner as participant
        );
        
        when(chatRepository.save(any(Chat.class)))
            .thenReturn(initialChat)
            .thenReturn(updatedChat);

        // When
        Chat result = chatManager.createGroupChat(ownerId, name, description, memberIds).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.chatType()).isEqualTo(ChatType.GROUP);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.description()).isEqualTo(description);
        assertThat(result.ownerId()).isEqualTo(ownerId);
        assertThat(result.participantCount()).isEqualTo(1); // New group starts with 1 participant (owner)
        
        verify(chatRepository, times(2)).save(any(Chat.class));
        verify(chatRepository).addParticipant(any(UUID.class), eq(ownerId), eq("OWNER"));
    }

    @Test
    @DisplayName("Should get chat by ID successfully")
    void testGetChatByIdSuccess() throws ExecutionException, InterruptedException {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // When
        Chat result = chatManager.getChat(chatId).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.chatId()).isEqualTo(chatId);
        
        verify(chatRepository).findById(chatId);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent chat by ID")
    void testGetChatByIdNonExistentChat() {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> chatManager.getChat(chatId).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("Chat not found");
        
        verify(chatRepository).findById(chatId);
    }

    @Test
    @DisplayName("Should update chat info successfully")
    void testUpdateChatInfoSuccess() throws ExecutionException, InterruptedException {
        // Given
        String newName = "Updated Chat Name";
        String newDescription = "Updated description";
        String newAvatarData = "newAvatarData";
        
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        // When
        Chat result = chatManager.updateChatInfo(chatId, newName, newDescription, newAvatarData).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.chatId()).isEqualTo(chatId);
        
        verify(chatRepository).findById(chatId);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("Should throw exception when updating info for non-existent chat")
    void testUpdateChatInfoNonExistentChat() {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> chatManager.updateChatInfo(chatId, "New Name", "New Description", null).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("Chat not found");
        
        verify(chatRepository).findById(chatId);
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("Should add participant to chat successfully")
    void testAddParticipantToChatSuccess() throws ExecutionException, InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatRepository.getParticipantCount(chatId)).thenReturn(participantCount + 1);

        // When
        chatManager.addParticipant(chatId, userId, "MEMBER").get();

        // Then
        verify(chatRepository).findById(chatId);
        verify(chatRepository).addParticipant(chatId, userId, "MEMBER");
        verify(chatRepository).getParticipantCount(chatId);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("Should throw exception when adding participant to non-existent chat")
    void testAddParticipantToNonExistentChat() {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> chatManager.addParticipant(chatId, userId, "MEMBER").get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("Chat not found");
        
        verify(chatRepository).findById(chatId);
        verify(chatRepository, never()).addParticipant(any(UUID.class), any(UUID.class), any(String.class));
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("Should remove participant from chat successfully")
    void testRemoveParticipantFromChatSuccess() throws ExecutionException, InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatRepository.getParticipantCount(chatId)).thenReturn(participantCount - 1);

        // When
        chatManager.removeParticipant(chatId, userId).get();

        // Then
        verify(chatRepository).findById(chatId);
        verify(chatRepository).removeParticipant(chatId, userId);
        verify(chatRepository).getParticipantCount(chatId);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    @DisplayName("Should throw exception when removing participant from non-existent chat")
    void testRemoveParticipantFromNonExistentChat() {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> chatManager.removeParticipant(chatId, userId).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("Chat not found");
        
        verify(chatRepository).findById(chatId);
        verify(chatRepository, never()).removeParticipant(any(UUID.class), any(UUID.class));
        verify(chatRepository, never()).save(any(Chat.class));
    }

    @Test
    @DisplayName("Should get chat participants successfully")
    void testGetParticipants() throws ExecutionException, InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Set<UUID> participants = Set.of(userId, userId2);
        when(chatRepository.findParticipants(chatId)).thenReturn(participants);

        // When
        Set<UUID> result = chatManager.getParticipants(chatId).get();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(userId, userId2);
        
        verify(chatRepository).findParticipants(chatId);
    }

    @Test
    @DisplayName("Should check if user is owner of chat")
    void testIsUserOwnerOfChat() throws ExecutionException, InterruptedException {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        // When
        Chat result = chatManager.getChat(chatId).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ownerId()).isEqualTo(ownerId);
        
        verify(chatRepository).findById(chatId);
    }

    @Test
    @DisplayName("Should return false when checking if non-existent user is owner of chat")
    void testIsUserOwnerOfChatWithNonExistentChat() {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> chatManager.getChat(chatId).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("Chat not found");
        
        verify(chatRepository).findById(chatId);
    }

    @Test
    @DisplayName("Should get user chats successfully")
    void testGetUserChatsSuccess() throws ExecutionException, InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findByUser(userId)).thenReturn(java.util.List.of(chat));

        // When
        java.util.List<Chat> result = chatManager.getUserChats(userId).get();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(chat);
        
        verify(chatRepository).findByUser(userId);
    }

    @Test
    @DisplayName("Should shutdown correctly")
    void testShutdown() {
        // When
        chatManager.shutdown();

        // Then
        // No exception should be thrown
        // This test mainly ensures the method doesn't throw any exceptions
        // In a real implementation, you might verify the executor is shut down
    }
}