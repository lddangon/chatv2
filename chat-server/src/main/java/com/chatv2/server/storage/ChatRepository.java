package com.chatv2.server.storage;

import com.chatv2.common.model.Chat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository interface for chat data access.
 */
public interface ChatRepository {
    /**
     * Saves a chat.
     */
    Chat save(Chat chat);

    /**
     * Finds a chat by ID.
     */
    Optional<Chat> findById(UUID chatId);

    /**
     * Finds all chats for a user.
     */
    List<Chat> findByUser(UUID userId);

    /**
     * Finds private chats between two users.
     */
    List<Chat> findPrivateChats(UUID user1Id, UUID user2Id);

    /**
     * Finds group chats.
     */
    List<Chat> findGroupChats();

    /**
     * Adds a participant to a chat.
     */
    void addParticipant(UUID chatId, UUID userId, String role);

    /**
     * Removes a participant from a chat.
     */
    void removeParticipant(UUID chatId, UUID userId);

    /**
     * Finds participants of a chat.
     */
    Set<UUID> findParticipants(UUID chatId);

    /**
     * Gets participant count for a chat.
     */
    int getParticipantCount(UUID chatId);

    /**
     * Checks if user is a participant.
     */
    boolean isParticipant(UUID chatId, UUID userId);

    /**
     * Deletes a chat by ID.
     */
    void deleteById(UUID chatId);

    /**
     * Counts all chats.
     */
    int countAll();

    /**
     * Finds all chats.
     */
    List<Chat> findAll();
}
