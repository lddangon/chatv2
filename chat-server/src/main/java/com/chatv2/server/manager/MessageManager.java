package com.chatv2.server.manager;

import com.chatv2.common.model.Message;
import com.chatv2.server.storage.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Message manager interface and implementation.
 * Handles message sending, receiving, and history.
 */
public class MessageManager {
    private static final Logger log = LoggerFactory.getLogger(MessageManager.class);
    private final MessageRepository messageRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MessageManager(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Sends a message to a chat.
     */
    public CompletableFuture<Message> sendMessage(UUID chatId, UUID senderId, String content, String messageType, UUID replyToId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Sending message from {} to chat {}", senderId, chatId);

                // Create message
                com.chatv2.common.model.MessageType type =
                    com.chatv2.common.model.MessageType.fromString(messageType);

                Message message;
                if (replyToId != null) {
                    message = Message.createReply(chatId, senderId, content, type, replyToId);
                } else {
                    message = Message.createNew(chatId, senderId, content, type);
                }

                // Save to database
                Message savedMessage = messageRepository.save(message);

                log.info("Message sent: {} in chat {}", savedMessage.messageId(), chatId);

                return savedMessage;
            } catch (Exception e) {
                log.error("Failed to send message", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets message history for a chat.
     */
    public CompletableFuture<List<Message>> getMessageHistory(UUID chatId, int limit, int offset, UUID beforeMessageId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting message history for chat {}: limit={}, offset={}", chatId, limit, offset);

                List<Message> messages;
                if (beforeMessageId != null) {
                    messages = messageRepository.findMessagesBefore(chatId, beforeMessageId, limit);
                } else {
                    messages = messageRepository.findMessagesByChat(chatId, limit, offset);
                }

                log.debug("Retrieved {} messages for chat {}", messages.size(), chatId);

                return messages;
            } catch (Exception e) {
                log.error("Failed to get message history", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets a message by ID.
     */
    public CompletableFuture<Message> getMessage(UUID messageId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting message: {}", messageId);

                Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.MESSAGE_NOT_FOUND,
                        "Message not found: " + messageId
                    ));

                return message;
            } catch (Exception e) {
                log.error("Failed to get message", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Edits a message.
     */
    public CompletableFuture<Message> editMessage(UUID messageId, UUID userId, String newContent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Editing message: {}", messageId);

                Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.MESSAGE_NOT_FOUND,
                        "Message not found: " + messageId
                    ));

                // Check if user is sender
                if (!message.senderId().equals(userId)) {
                    throw new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.FORBIDDEN,
                        "You can only edit your own messages"
                    );
                }

                // Update content
                Message editedMessage = message.withEditedContent(newContent);
                Message savedMessage = messageRepository.save(editedMessage);

                log.info("Message edited: {}", messageId);

                return savedMessage;
            } catch (Exception e) {
                log.error("Failed to edit message", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Deletes a message.
     */
    public CompletableFuture<Message> deleteMessage(UUID messageId, UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Deleting message: {}", messageId);

                Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.MESSAGE_NOT_FOUND,
                        "Message not found: " + messageId
                    ));

                // Check if user is sender
                if (!message.senderId().equals(userId)) {
                    throw new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.FORBIDDEN,
                        "You can only delete your own messages"
                    );
                }

                // Mark as deleted
                Message deletedMessage = message.asDeleted();
                Message savedMessage = messageRepository.save(deletedMessage);

                log.info("Message deleted: {}", messageId);

                return savedMessage;
            } catch (Exception e) {
                log.error("Failed to delete message", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Marks a message as read by a user.
     */
    public CompletableFuture<Void> markAsRead(UUID messageId, UUID userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Marking message {} as read by {}", messageId, userId);

                Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.MESSAGE_NOT_FOUND,
                        "Message not found: " + messageId
                    ));

                // Update read receipts
                messageRepository.addReadReceipt(messageId, userId);

                log.debug("Message {} marked as read by {}", messageId, userId);
            } catch (Exception e) {
                log.error("Failed to mark message as read", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets unread messages for a user in a chat.
     */
    public CompletableFuture<List<Message>> getUnreadMessages(UUID chatId, UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Getting unread messages for chat {} and user {}", chatId, userId);

                List<Message> messages = messageRepository.findUnreadMessages(chatId, userId);

                log.debug("Found {} unread messages", messages.size());

                return messages;
            } catch (Exception e) {
                log.error("Failed to get unread messages", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Shuts down executor.
     */
    public void shutdown() {
        log.info("Shutting down MessageManager");
        executor.shutdown();
    }
}
