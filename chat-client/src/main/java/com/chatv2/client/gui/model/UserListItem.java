package com.chatv2.client.gui.model;

import com.chatv2.common.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Record for displaying user information in ListView components.
 * Used by UserListCell for rendering users in participant lists.
 */
public record UserListItem(
    UUID userId,
    String username,
    UserStatus status,
    byte[] avatar,
    Instant lastActivity
) {
    /**
     * Creates a new UserListItem with default values.
     */
    public UserListItem {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be null or blank");
        }
        if (status == null) {
            status = UserStatus.OFFLINE;
        }
        if (lastActivity == null) {
            lastActivity = Instant.EPOCH;
        }
    }

    /**
     * Creates a UserListItem from a minimal set of required fields.
     */
    public static UserListItem create(UUID userId, String username, UserStatus status) {
        return new UserListItem(userId, username, status, null, Instant.EPOCH);
    }

    /**
     * Creates a UserListItem with avatar data.
     */
    public static UserListItem withAvatar(
        UUID userId,
        String username,
        UserStatus status,
        byte[] avatar
    ) {
        return new UserListItem(userId, username, status, avatar, Instant.EPOCH);
    }

    /**
     * Creates a UserListItem with all fields.
     */
    public static UserListItem full(
        UUID userId,
        String username,
        UserStatus status,
        byte[] avatar,
        Instant lastActivity
    ) {
        return new UserListItem(userId, username, status, avatar, lastActivity);
    }

    /**
     * Returns a copy with updated status.
     */
    public UserListItem withStatus(UserStatus newStatus) {
        return new UserListItem(this.userId, this.username, newStatus, this.avatar, this.lastActivity);
    }

    /**
     * Returns a copy with updated avatar.
     */
    public UserListItem withAvatar(byte[] newAvatar) {
        return new UserListItem(this.userId, this.username, this.status, newAvatar, this.lastActivity);
    }

    /**
     * Returns a copy with updated last activity timestamp.
     */
    public UserListItem withLastActivity(Instant newLastActivity) {
        return new UserListItem(this.userId, this.username, this.status, this.avatar, newLastActivity);
    }

    /**
     * Checks if user has an avatar.
     */
    public boolean hasAvatar() {
        return avatar != null && avatar.length > 0;
    }

    /**
     * Returns the display name (username for now, could include full name).
     */
    public String getDisplayName() {
        return username;
    }
}
