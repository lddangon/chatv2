package com.chatv2.server.storage;

import com.chatv2.common.model.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for message data access.
 */
public interface MessageRepository {
    /**
     * Saves a message.
     */
    Message save(Message message);

    /**
     * Finds a message by ID.
     */
    Optional<Message> findById(UUID messageId);

    /**
     * Finds messages by chat ID.
     */
    List<Message> findMessagesByChat(UUID chatId, int limit, int offset);

    /**
     * Finds messages before a given message.
     */
    List<Message> findMessagesBefore(UUID chatId, UUID beforeMessageId, int limit);

    /**
     * Finds unread messages for a user in a chat.
     */
    List<Message> findUnreadMessages(UUID chatId, UUID userId);

    /**
     * Finds messages sent by a user.
     */
    List<Message> findMessagesBySender(UUID senderId, int limit);

    /**
     * Finds recent messages across all chats.
     */
    List<Message> findRecentMessages(int limit);

    /**
     * Adds a read receipt for a message.
     */
    void addReadReceipt(UUID messageId, UUID userId);

    /**
     * Gets read receipts for a message.
     */
    List<UUID> getReadReceipts(UUID messageId);

    /**
     * Deletes messages by chat ID.
     */
    void deleteByChatId(UUID chatId);

    /**
     * Deletes a message by ID.
     */
    void delete(UUID messageId);

    /**
     * Counts messages in a chat.
     */
    long countByChatId(UUID chatId);

    /**
     * Counts all messages.
     */
    int countAll();

    /**
     * Counts messages sent by a user after a specific date.
     */
    int countByUserAfterDate(UUID userId, java.time.Instant date);
}
