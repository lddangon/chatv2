package com.chatv2.client.core;

/**
 * Enum representing client connection state.
 */
public enum ConnectionState {
    DISCONNECTED("Disconnected", false),
    CONNECTING("Connecting...", false),
    CONNECTED("Connected", true),
    AUTHENTICATED("Authenticated", true),
    RECONNECTING("Reconnecting...", false),
    ERROR("Error", false);

    private final String displayName;
    private final boolean isConnected;

    ConnectionState(String displayName, boolean isConnected) {
        this.displayName = displayName;
        this.isConnected = isConnected;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
