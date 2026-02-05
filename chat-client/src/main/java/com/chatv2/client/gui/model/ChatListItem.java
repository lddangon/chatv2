package com.chatv2.client.gui.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Record for displaying chat information in ListView.
 */
public record ChatListItem(
    UUID chatId,
    String name,
    String lastMessage,
    Instant lastMessageTime,
    int unreadCount,
    byte[] avatar
) {
    /**
     * Creates a chat list item.
     */
    public ChatListItem {
        if (chatId == null) {
            throw new IllegalArgumentException("Chat ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            name = "Unnamed Chat";
        }
        if (lastMessage == null) {
            lastMessage = "";
        }
        if (lastMessageTime == null) {
            lastMessageTime = Instant.now();
        }
        if (unreadCount < 0) {
            unreadCount = 0;
        }
    }

    /**
     * Gets display text for last message.
     */
    public String getDisplayLastMessage() {
        if (lastMessage == null || lastMessage.isBlank()) {
            return "No messages yet";
        }
        if (lastMessage.length() > 30) {
            return lastMessage.substring(0, 30) + "...";
        }
        return lastMessage;
    }

    /**
     * Gets formatted time for display.
     */
    public String getFormattedTime() {
        if (lastMessageTime == null) {
            return "";
        }
        long seconds = Instant.now().getEpochSecond() - lastMessageTime.getEpochSecond();
        if (seconds < 60) {
            return "Just now";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m ago";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "h ago";
        } else {
            return (seconds / 86400) + "d ago";
        }
    }
}
