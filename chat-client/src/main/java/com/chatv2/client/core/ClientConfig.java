package com.chatv2.client.core;

import java.time.Instant;

/**
 * Client configuration record.
 */
public record ClientConfig(
    String name,
    String version,
    boolean discoveryEnabled,
    String udpMulticastAddress,
    int udpMulticastPort,
    int discoveryTimeoutSeconds,
    int reconnectAttempts,
    int reconnectDelaySeconds,
    int heartbeatIntervalSeconds,
    boolean encryptionEnabled
) {
    /**
     * Creates default client configuration.
     */
    public ClientConfig() {
        this(
            "ChatV2 Client",
            "1.0.0",
            true,
            "239.255.255.250",
            9999,
            30,
            5,
            5,
            30,
            true
        );
    }

    public ClientConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name cannot be null or blank");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Client version cannot be null or blank");
        }
        if (reconnectAttempts < 0) {
            throw new IllegalArgumentException("Reconnect attempts cannot be negative");
        }
        if (reconnectDelaySeconds < 0) {
            throw new IllegalArgumentException("Reconnect delay cannot be negative");
        }
        if (heartbeatIntervalSeconds < 1) {
            throw new IllegalArgumentException("Heartbeat interval must be positive");
        }
        if (discoveryTimeoutSeconds < 1) {
            discoveryTimeoutSeconds = 30;
        }
    }
}
