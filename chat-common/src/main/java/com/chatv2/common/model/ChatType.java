package com.chatv2.common.model;

/**
 * Enum representing chat type.
 */
public enum ChatType {
    PRIVATE("Private", "1-on-1 conversation"),
    GROUP("Group", "Multiple participants");

    private final String displayName;
    private final String description;

    ChatType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static ChatType fromString(String type) {
        try {
            return ChatType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PRIVATE;
        }
    }
}
