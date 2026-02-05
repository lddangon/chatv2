package com.chatv2.common.model;

/**
 * Enum representing user status.
 */
public enum UserStatus {
    ONLINE("Online", "#00FF00"),
    OFFLINE("Offline", "#808080"),
    AWAY("Away", "#FFFF00"),
    BUSY("Busy", "#FF0000"),
    INVISIBLE("Invisible", "#808080");

    private final String displayName;
    private final String color;

    UserStatus(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public static UserStatus fromString(String status) {
        try {
            return UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OFFLINE;
        }
    }
}
