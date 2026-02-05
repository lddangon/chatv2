package com.chatv2.client.gui.model;

import com.chatv2.common.model.UserStatus;

import java.util.UUID;

/**
 * Record for displaying participant information in ListView.
 */
public record ParticipantListItem(
    UUID userId,
    String username,
    UserStatus status,
    byte[] avatar
) {
    /**
     * Creates a participant list item.
     */
    public ParticipantListItem {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (username == null || username.isBlank()) {
            username = "Unknown User";
        }
        if (status == null) {
            status = UserStatus.OFFLINE;
        }
    }

    /**
     * Gets status display name.
     */
    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "Offline";
    }
}
