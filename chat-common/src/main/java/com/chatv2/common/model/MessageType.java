package com.chatv2.common.model;

/**
 * Enum representing message type.
 */
public enum MessageType {
    TEXT("Text", "Plain text message"),
    IMAGE("Image", "Image message with base64 data"),
    FILE("File", "File attachment"),
    VOICE("Voice", "Voice message"),
    SYSTEM("System", "System notification");

    private final String displayName;
    private final String description;

    MessageType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static MessageType fromString(String type) {
        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}
