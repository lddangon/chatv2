package com.chatv2.server.manager;

import com.chatv2.common.model.Chat;
import com.chatv2.common.model.ChatType;
import com.chatv2.server.storage.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chat manager interface and implementation.
 * Handles chat creation, participant management, and chat metadata.
 */
public class ChatManager {
    private static final Logger log = LoggerFactory.getLogger(ChatManager.class);
    private final ChatRepository chatRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatManager(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    /**
     * Creates a private chat between two users.
     */
    public CompletableFuture<Chat> createPrivateChat(UUID user1Id, UUID user2Id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Creating private chat between {} and {}", user1Id, user2Id);

                // Check if private chat already exists
                List<Chat> existingChats = chatRepository.findPrivateChats(user1Id, user2Id);
                if (!existingChats.isEmpty()) {
                    return existingChats.get(0);
                }

                // Create new private chat
                Chat chat = Chat.createPrivateChat(user1Id);
                Chat savedChat = chatRepository.save(chat);

                // Add both users as participants
                chatRepository.addParticipant(savedChat.chatId(), user1Id, "OWNER");
                chatRepository.addParticipant(savedChat.chatId(), user2Id, "MEMBER");

                // Update participant count
                savedChat = savedChat.withParticipantCount(2);
                savedChat = chatRepository.save(savedChat);

                log.info("Private chat created: {}", savedChat.chatId());

                return savedChat;
            } catch (Exception e) {
                log.error("Failed to create private chat", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Creates a group chat.
     */
    public CompletableFuture<Chat> createGroupChat(UUID ownerId, String name, String description, Set<UUID> memberIds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Creating group chat '{}' by owner {}", name, ownerId);

                // Create group chat
                Chat chat = Chat.createGroupChat(ownerId, name, description, null);
                Chat savedChat = chatRepository.save(chat);

                // Add owner as participant
                chatRepository.addParticipant(savedChat.chatId(), ownerId, "OWNER");

                // Add other members
                for (UUID memberId : memberIds) {
                    if (!memberId.equals(ownerId)) {
                        chatRepository.addParticipant(savedChat.chatId(), memberId, "MEMBER");
                    }
                }

                // Update participant count
                savedChat = savedChat.withParticipantCount(1 + memberIds.size());
                savedChat = chatRepository.save(savedChat);

                log.info("Group chat created: {} with {} participants", savedChat.chatId(), savedChat.participantCount());

                return savedChat;
            } catch (Exception e) {
                log.error("Failed to create group chat", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets all chats for a user.
     */
    public CompletableFuture<List<Chat>> getUserChats(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Getting chats for user: {}", userId);

            List<Chat> chats = chatRepository.findByUser(userId);

            log.debug("Found {} chats for user: {}", chats.size(), userId);

            return chats;
        }, executor);
    }

    /**
     * Adds a participant to a chat.
     */
    public CompletableFuture<Void> addParticipant(UUID chatId, UUID userId, String role) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Adding user {} to chat {} as {}", userId, chatId, role);

                Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.CHAT_NOT_FOUND,
                        "Chat not found: " + chatId
                    ));

                chatRepository.addParticipant(chatId, userId, role);

                // Update participant count
                int newCount = chatRepository.getParticipantCount(chatId);
                Chat updatedChat = chat.withParticipantCount(newCount);
                chatRepository.save(updatedChat);

                log.info("User {} added to chat {}", userId, chatId);
            } catch (Exception e) {
                log.error("Failed to add participant", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Removes a participant from a chat.
     */
    public CompletableFuture<Void> removeParticipant(UUID chatId, UUID userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Removing user {} from chat {}", userId, chatId);

                Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.CHAT_NOT_FOUND,
                        "Chat not found: " + chatId
                    ));

                chatRepository.removeParticipant(chatId, userId);

                // Update participant count
                int newCount = chatRepository.getParticipantCount(chatId);
                Chat updatedChat = chat.withParticipantCount(newCount);
                chatRepository.save(updatedChat);

                log.info("User {} removed from chat {}", userId, chatId);
            } catch (Exception e) {
                log.error("Failed to remove participant", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets chat participants.
     */
    public CompletableFuture<Set<UUID>> getParticipants(UUID chatId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Getting participants for chat: {}", chatId);

            Set<UUID> participants = chatRepository.findParticipants(chatId);

            log.debug("Found {} participants for chat {}", participants.size(), chatId);

            return participants;
        }, executor);
    }

    /**
     * Updates chat information.
     */
    public CompletableFuture<Chat> updateChatInfo(UUID chatId, String name, String description, String avatarData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Updating chat info for: {}", chatId);

                Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.CHAT_NOT_FOUND,
                        "Chat not found: " + chatId
                    ));

                Chat updatedChat = chat.withUpdates(name, description, avatarData);
                Chat savedChat = chatRepository.save(updatedChat);

                log.info("Chat info updated: {}", chatId);

                return savedChat;
            } catch (Exception e) {
                log.error("Failed to update chat info", e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets chat by ID.
     */
    public CompletableFuture<Chat> getChat(UUID chatId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Getting chat: {}", chatId);

            Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                    com.chatv2.common.exception.ChatException.CHAT_NOT_FOUND,
                    "Chat not found: " + chatId
                ));

            return chat;
        }, executor);
    }

    /**
     * Shuts down executor.
     */
    public void shutdown() {
        log.info("Shutting down ChatManager");
        executor.shutdown();
    }
}
