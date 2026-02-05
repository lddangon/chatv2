package com.chatv2.server.core;

import java.time.Instant;
import java.util.UUID;

/**
 * Server configuration class.
 */
public class ServerConfig {
    private final String host;
    private final int port;
    private final String name;
    private final String databasePath;
    private final int connectionPoolSize;
    private final boolean encryptionRequired;
    private final int rsaKeySize;
    private final int aesKeySize;
    private final String udpMulticastAddress;
    private final int udpMulticastPort;
    private final boolean udpEnabled;
    private final int sessionExpirationSeconds;
    private final int tokenExpirationSeconds;

    /**
     * Creates default server configuration.
     */
    public ServerConfig() {
        this(
            "0.0.0.0",
            8080,
            "ChatV2 Server",
            "data/chat.db",
            10,
            true,
            4096,
            256,
            "239.255.255.250",
            9999,
            true,
            3600,
            3600
        );
    }

    /**
     * Creates server configuration with custom host and port.
     */
    public ServerConfig(String host, int port) {
        this(
            host,
            port,
            "ChatV2 Server",
            "data/chat.db",
            10,
            true,
            4096,
            256,
            "239.255.255.250",
            9999,
            true,
            3600,
            3600
        );
    }

    public ServerConfig(
        String host,
        int port,
        String name,
        String databasePath,
        int connectionPoolSize,
        boolean encryptionRequired,
        int rsaKeySize,
        int aesKeySize,
        String udpMulticastAddress,
        int udpMulticastPort,
        boolean udpEnabled,
        int sessionExpirationSeconds,
        int tokenExpirationSeconds
    ) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be null or blank");
        }
        this.host = host;
        
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.port = port;
        
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Server name cannot be null or blank");
        }
        this.name = name;
        
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException("Database path cannot be null or blank");
        }
        this.databasePath = databasePath;
        
        if (connectionPoolSize < 1) {
            throw new IllegalArgumentException("Connection pool size must be positive");
        }
        this.connectionPoolSize = connectionPoolSize;
        
        this.encryptionRequired = encryptionRequired;
        
        if (rsaKeySize < 1024) {
            throw new IllegalArgumentException("RSA key size must be at least 1024 bits");
        }
        this.rsaKeySize = rsaKeySize;
        
        if (aesKeySize < 128) {
            throw new IllegalArgumentException("AES key size must be at least 128 bits");
        }
        this.aesKeySize = aesKeySize;
        
        this.udpMulticastAddress = udpMulticastAddress;
        this.udpMulticastPort = udpMulticastPort;
        this.udpEnabled = udpEnabled;
        
        if (sessionExpirationSeconds < 60) {
            sessionExpirationSeconds = 3600;
        }
        this.sessionExpirationSeconds = sessionExpirationSeconds;
        
        if (tokenExpirationSeconds < 60) {
            tokenExpirationSeconds = 3600;
        }
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }

    // Getters
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public boolean isEncryptionRequired() {
        return encryptionRequired;
    }

    public int getRsaKeySize() {
        return rsaKeySize;
    }

    public int getAesKeySize() {
        return aesKeySize;
    }

    public String getUdpMulticastAddress() {
        return udpMulticastAddress;
    }

    public int getUdpMulticastPort() {
        return udpMulticastPort;
    }

    public boolean isUdpEnabled() {
        return udpEnabled;
    }

    public int getSessionExpirationSeconds() {
        return sessionExpirationSeconds;
    }

    public int getTokenExpirationSeconds() {
        return tokenExpirationSeconds;
    }
}