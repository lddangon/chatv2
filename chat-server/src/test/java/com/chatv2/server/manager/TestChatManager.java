import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class TestChatManager {

    @Mock
    private com.chatv2.server.storage.ChatRepository chatRepository;

    @InjectMocks
    private com.chatv2.server.manager.ChatManager chatManager;

    private UUID chatId;
    private UUID ownerId;
    private String name;
    private String description;
    private String avatarData;
    private int participantCount;
    private com.chatv2.common.model.Chat chat;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        name = "Test Chat";
        description = "Test chat description";
        avatarData = "avatarData";
        participantCount = 2;
        
        chat = new com.chatv2.common.model.Chat(
                chatId, com.chatv2.common.model.ChatType.GROUP, name, description, ownerId, avatarData,
                java.time.Instant.now(), java.time.Instant.now(), participantCount
        );
    }

    @Test
    @DisplayName("Should add participant to chat successfully")
    void testAddParticipantToChatSuccess() throws ExecutionException, InterruptedException {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatRepository.getParticipantCount(chatId)).thenReturn(participantCount + 1);

        // When
        chatManager.addParticipant(chatId, UUID.randomUUID(), "MEMBER").get();

        // Then
        verify(chatRepository).findById(eq(chatId));
        verify(chatRepository).addParticipant(eq(chatId), any(UUID.class), eq("MEMBER"));
        verify(chatRepository).getParticipantCount(eq(chatId));
        verify(chatRepository).save(any(com.chatv2.common.model.Chat.class));
    }

    @Test
    @DisplayName("Should throw exception when adding participant to non-existent chat")
    void testAddParticipantToNonExistentChat() {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        try {
            chatManager.addParticipant(chatId, userId, "MEMBER").get();
        } catch (Exception e) {
            // Expected
        }

        verify(chatRepository).findById(chatId);
        verify(chatRepository, never()).addParticipant(any(UUID.class), any(UUID.class), any(String.class));
        verify(chatRepository, never()).save(any(com.chatv2.common.model.Chat.class));
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
        verify(chatRepository).save(any(com.chatv2.common.model.Chat.class));
    }

    @Test
    @DisplayName("Should throw exception when removing participant from non-existent chat")
    void testRemoveParticipantFromNonExistentChat() {
        // Given
        UUID userId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When/Then
        try {
            chatManager.removeParticipant(chatId, userId).get();
        } catch (Exception e) {
            // Expected
        }

        verify(chatRepository).findById(chatId);
        verify(chatRepository, never()).removeParticipant(any(UUID.class), any(UUID.class));
        verify(chatRepository, never()).save(any(com.chatv2.common.model.Chat.class));
    }

    /*public static void main(String[] args) {
        org.junit.platform.console.ConsoleLauncher.main(new String[]{
            "--class-path", System.getProperty("java.class.path"),
            "--select-class", TestChatManager.class.getName()
        });
    }*/
}