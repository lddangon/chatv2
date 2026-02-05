package com.chatv2.client.discovery;

import java.net.InetAddress;
import java.time.Instant;

/**
 * Record containing discovered server information.
 */
public record ServerInfo(
    String serverId,
    String serverName,
    String address,
    int port,
    String version,
    boolean encryptionRequired,
    String encryptionType,
    int currentUsers,
    int maxUsers,
    Instant discoveredAt
) {
    /**
     * Creates a server info record.
     */
    public ServerInfo {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("Server ID cannot be null or blank");
        }
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("Server name cannot be null or blank");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version cannot be null or blank");
        }
        if (currentUsers < 0) {
            currentUsers = 0;
        }
        if (maxUsers < 1) {
            maxUsers = 1000;
        }
        if (discoveredAt == null) {
            discoveredAt = Instant.now();
        }
    }

    /**
     * Checks if server is full.
     */
    public boolean isFull() {
        return currentUsers >= maxUsers;
    }

    /**
     * Gets server load as percentage.
     */
    public double getLoadPercentage() {
        return (double) currentUsers / maxUsers * 100;
    }

    /**
     * Gets server display string.
     */
    public String getDisplayString() {
        return String.format("%s (%s:%d) - %d/%d users",
            serverName, address, port, currentUsers, maxUsers);
    }

    /**
     * Checks if server info is recent (discovered within last 60 seconds).
     */
    public boolean isRecent() {
        return Instant.now().getEpochSecond() - discoveredAt.getEpochSecond() < 60;
    }
}
