package com.chatv2.common.discovery;

import java.util.UUID;

/**
 * Discovery packet record for UDP multicast server discovery.
 * <p>
 * Contains server information that is broadcast to clients for auto-discovery.
 * This record is used by both the server broadcaster and the client discovery mechanism.
 * </p>
 * <p>
 * The packet includes server identification, network details, capacity information,
 * and encryption configuration. It is serialized to JSON for transmission over UDP multicast.
 * </p>
 */
public record DiscoveryPacket(
    /**
     * Unique server identifier (UUID).
     * If null or blank on construction, a random UUID is generated.
     */
    String serverId,

    /**
     * Human-readable server name.
     * Must not be null or blank.
     */
    String serverName,

    /**
     * Server network address (IP address or hostname).
     * Must not be null or blank.
     */
    String address,

    /**
     * Server port number.
     * Must be between 1 and 65535.
     */
    int port,

    /**
     * Server protocol version.
     * Defaults to "1.0.0" if null or blank.
     */
    String version,

    /**
     * Maximum number of users the server can support.
     * Must be at least 1. Defaults to 1000 if invalid.
     */
    int maxUsers,

    /**
     * Current number of connected users.
     * Must be non-negative. Defaults to 0 if negative.
     */
    int currentUsers,

    /**
     * Whether encryption is required for connections.
     */
    boolean encryptionRequired,

    /**
     * Type of encryption being used (e.g., "AES", "RSA").
     * May be null if encryption is not required.
     */
    String encryptionType,

    /**
     * Current server state (e.g., "ACTIVE", "MAINTENANCE", "FULL").
     * Defaults to "ACTIVE" if null or blank.
     */
    String state
) {
    /**
     * Creates a discovery packet with validation and default values.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>serverId: defaults to random UUID if null or blank</li>
     *   <li>serverName: must not be null or blank (throws IllegalArgumentException)</li>
     *   <li>address: must not be null or blank (throws IllegalArgumentException)</li>
     *   <li>port: must be between 1 and 65535 (throws IllegalArgumentException)</li>
     *   <li>version: defaults to "1.0.0" if null or blank</li>
     *   <li>maxUsers: defaults to 1000 if less than 1</li>
     *   <li>currentUsers: defaults to 0 if negative</li>
     *   <li>state: defaults to "ACTIVE" if null or blank</li>
     * </ul>
     * </p>
     *
     * @throws IllegalArgumentException if serverName or address is null/blank, or if port is invalid
     */
    public DiscoveryPacket {
        if (serverId == null || serverId.isBlank()) {
            serverId = UUID.randomUUID().toString();
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
            version = "1.0.0";
        }
        if (maxUsers < 1) {
            maxUsers = 1000;
        }
        if (currentUsers < 0) {
            currentUsers = 0;
        }
        if (state == null || state.isBlank()) {
            state = "ACTIVE";
        }
    }

    /**
     * Checks if the server is at maximum capacity.
     *
     * @return true if currentUsers >= maxUsers, false otherwise
     */
    public boolean isFull() {
        return currentUsers >= maxUsers;
    }

    /**
     * Calculates the current server load as a percentage.
     *
     * @return load percentage as a value between 0.0 and 100.0
     */
    public double getLoadPercentage() {
        return (double) currentUsers / maxUsers * 100.0;
    }

    /**
     * Generates a human-readable display string for the server.
     * <p>
     * Format: "{serverName} ({address}:{port}) - {currentUsers}/{maxUsers} users [{state}]"
     * </p>
     *
     * @return formatted display string
     */
    public String getDisplayString() {
        return String.format("%s (%s:%d) - %d/%d users [%s]",
            serverName, address, port, currentUsers, maxUsers, state);
    }

    /**
     * Checks if the server is accepting new connections.
     * <p>
     * A server is accepting connections if it is ACTIVE and not full.
     * </p>
     *
     * @return true if the server is accepting connections, false otherwise
     */
    public boolean isAcceptingConnections() {
        return "ACTIVE".equals(state) && !isFull();
    }

    /**
     * Validates that the packet contains all required information.
     * <p>
     * This is a stricter validation than the constructor validation,
     * checking for business logic correctness.
     * </p>
     *
     * @return true if the packet is valid, false otherwise
     */
    public boolean isValid() {
        if (serverId == null || serverId.isBlank()) {
            return false;
        }
        if (serverName == null || serverName.isBlank()) {
            return false;
        }
        if (address == null || address.isBlank()) {
            return false;
        }
        if (port < 1 || port > 65535) {
            return false;
        }
        if (state == null || state.isBlank()) {
            return false;
        }
        if (encryptionRequired && (encryptionType == null || encryptionType.isBlank())) {
            return false;
        }
        if (currentUsers > maxUsers) {
            return false;
        }
        return true;
    }
}
