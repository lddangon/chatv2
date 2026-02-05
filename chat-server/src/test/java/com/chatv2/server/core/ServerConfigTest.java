package com.chatv2.server.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerConfigTest {

    @Test
    @DisplayName("Should create default server configuration")
    void testDefaultServerConfig() {
        // When
        ServerConfig config = new ServerConfig();

        // Then
        assertThat(config.getHost()).isEqualTo("0.0.0.0");
        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getName()).isEqualTo("ChatV2 Server");
        assertThat(config.getDatabasePath()).isEqualTo("data/chat.db");
        assertThat(config.getConnectionPoolSize()).isEqualTo(10);
        assertThat(config.isEncryptionRequired()).isTrue();
        assertThat(config.getRsaKeySize()).isEqualTo(4096);
        assertThat(config.getAesKeySize()).isEqualTo(256);
        assertThat(config.getUdpMulticastAddress()).isEqualTo("239.255.255.250");
        assertThat(config.getUdpMulticastPort()).isEqualTo(9999);
        assertThat(config.isUdpEnabled()).isTrue();
        assertThat(config.getSessionExpirationSeconds()).isEqualTo(3600);
        assertThat(config.getTokenExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Should create server configuration with custom host and port")
    void testServerConfigWithCustomHostAndPort() {
        // Given
        String customHost = "192.168.1.100";
        int customPort = 9090;

        // When
        ServerConfig config = new ServerConfig(customHost, customPort);

        // Then
        assertThat(config.getHost()).isEqualTo(customHost);
        assertThat(config.getPort()).isEqualTo(customPort);
        assertThat(config.getName()).isEqualTo("ChatV2 Server");
        assertThat(config.getDatabasePath()).isEqualTo("data/chat.db");
        assertThat(config.getConnectionPoolSize()).isEqualTo(10);
        assertThat(config.isEncryptionRequired()).isTrue();
        assertThat(config.getRsaKeySize()).isEqualTo(4096);
        assertThat(config.getAesKeySize()).isEqualTo(256);
        assertThat(config.getUdpMulticastAddress()).isEqualTo("239.255.255.250");
        assertThat(config.getUdpMulticastPort()).isEqualTo(9999);
        assertThat(config.isUdpEnabled()).isTrue();
        assertThat(config.getSessionExpirationSeconds()).isEqualTo(3600);
        assertThat(config.getTokenExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Should create server configuration with all parameters")
    void testServerConfigWithAllParameters() {
        // Given
        String host = "192.168.1.100";
        int port = 9090;
        String name = "Test Server";
        String databasePath = "/opt/chat/data.db";
        int connectionPoolSize = 20;
        boolean encryptionRequired = false;
        int rsaKeySize = 2048;
        int aesKeySize = 128;
        String udpMulticastAddress = "224.0.0.1";
        int udpMulticastPort = 8888;
        boolean udpEnabled = false;
        int sessionExpirationSeconds = 1800;
        int tokenExpirationSeconds = 900;

        // When
        ServerConfig config = new ServerConfig(
                host, port, name, databasePath, connectionPoolSize, encryptionRequired,
                rsaKeySize, aesKeySize, udpMulticastAddress, udpMulticastPort, udpEnabled,
                sessionExpirationSeconds, tokenExpirationSeconds
        );

        // Then
        assertThat(config.getHost()).isEqualTo(host);
        assertThat(config.getPort()).isEqualTo(port);
        assertThat(config.getName()).isEqualTo(name);
        assertThat(config.getDatabasePath()).isEqualTo(databasePath);
        assertThat(config.getConnectionPoolSize()).isEqualTo(connectionPoolSize);
        assertThat(config.isEncryptionRequired()).isEqualTo(encryptionRequired);
        assertThat(config.getRsaKeySize()).isEqualTo(rsaKeySize);
        assertThat(config.getAesKeySize()).isEqualTo(aesKeySize);
        assertThat(config.getUdpMulticastAddress()).isEqualTo(udpMulticastAddress);
        assertThat(config.getUdpMulticastPort()).isEqualTo(udpMulticastPort);
        assertThat(config.isUdpEnabled()).isEqualTo(udpEnabled);
        assertThat(config.getSessionExpirationSeconds()).isEqualTo(sessionExpirationSeconds);
        assertThat(config.getTokenExpirationSeconds()).isEqualTo(tokenExpirationSeconds);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw exception when host is null or blank")
    void testServerConfigWithInvalidHost(String host) {
        // Given
        int port = 8080;

        // When/Then
        assertThatThrownBy(() -> new ServerConfig(host, port))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Host cannot be null or blank");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 65536})
    @DisplayName("Should throw exception when port is out of valid range")
    void testServerConfigWithInvalidPort(int port) {
        // Given
        String host = "localhost";

        // When/Then
        assertThatThrownBy(() -> new ServerConfig(host, port))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Port must be between 1 and 65535");
    }

    @Test
    @DisplayName("Should accept valid port values")
    void testServerConfigWithValidPort() {
        // Given
        String host = "localhost";
        int[] validPorts = {1, 8080, 65535};

        for (int port : validPorts) {
            // When
            ServerConfig config = new ServerConfig(host, port);

            // Then
            assertThat(config.getPort()).isEqualTo(port);
        }
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw exception when name is null or blank")
    void testServerConfigWithNullName(String name) {
        // When/Then
        assertThatThrownBy(() -> new ServerConfig(
                "localhost", 8080, name, "data/chat.db", 10, true,
                4096, 256, "239.255.255.250", 9999, true,
                3600, 3600
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Server name cannot be null or blank");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw exception when database path is null or blank")
    void testServerConfigWithInvalidDatabasePath(String databasePath) {
        // When/Then
        assertThatThrownBy(() -> new ServerConfig(
                "localhost", 8080, "Test Server", databasePath, 10, true,
                4096, 256, "239.255.255.250", 9999, true,
                3600, 3600
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Database path cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when connection pool size is non-positive")
    void testServerConfigWithInvalidConnectionPoolSize() {
        // When/Then
        assertThatThrownBy(() -> new ServerConfig(
                "localhost", 8080, "Test Server", "data/chat.db", 0, true,
                4096, 256, "239.255.255.250", 9999, true,
                3600, 3600
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Connection pool size must be positive");
    }

    @Test
    @DisplayName("Should throw exception when RSA key size is too small")
    void testServerConfigWithInvalidRsaKeySize() {
        // When/Then
        assertThatThrownBy(() -> new ServerConfig(
                "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                1023, 256, "239.255.255.250", 9999, true,
                3600, 3600
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("RSA key size must be at least 1024 bits");
    }

    @Test
    @DisplayName("Should accept valid RSA key sizes")
    void testServerConfigWithValidRsaKeySize() {
        // Given
        int[] validRsaKeySizes = {1024, 2048, 4096};

        for (int rsaKeySize : validRsaKeySizes) {
            // When
            ServerConfig config = new ServerConfig(
                    "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                    rsaKeySize, 256, "239.255.255.250", 9999, true,
                    3600, 3600
            );

            // Then
            assertThat(config.getRsaKeySize()).isEqualTo(rsaKeySize);
        }
    }

    @Test
    @DisplayName("Should throw exception when AES key size is too small")
    void testServerConfigWithInvalidAesKeySize() {
        // When/Then
        assertThatThrownBy(() -> new ServerConfig(
                "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                4096, 127, "239.255.255.250", 9999, true,
                3600, 3600
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AES key size must be at least 128 bits");
    }

    @Test
    @DisplayName("Should accept valid AES key sizes")
    void testServerConfigWithValidAesKeySize() {
        // Given
        int[] validAesKeySizes = {128, 192, 256};

        for (int aesKeySize : validAesKeySizes) {
            // When
            ServerConfig config = new ServerConfig(
                    "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                    4096, aesKeySize, "239.255.255.250", 9999, true,
                    3600, 3600
            );

            // Then
            assertThat(config.getAesKeySize()).isEqualTo(aesKeySize);
        }
    }

    @Test
    @DisplayName("Should use default session expiration when value is too small")
    void testServerConfigWithTooSmallSessionExpiration() {
        // When
        ServerConfig config = new ServerConfig(
                "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                4096, 256, "239.255.255.250", 9999, true,
                30, 3600
        );

        // Then
        assertThat(config.getSessionExpirationSeconds()).isEqualTo(3600); // Default value
    }

    @Test
    @DisplayName("Should use default token expiration when value is too small")
    void testServerConfigWithTooSmallTokenExpiration() {
        // When
        ServerConfig config = new ServerConfig(
                "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                4096, 256, "239.255.255.250", 9999, true,
                3600, 30
        );

        // Then
        assertThat(config.getTokenExpirationSeconds()).isEqualTo(3600); // Default value
    }

    @Test
    @DisplayName("Should accept valid session and token expiration values")
    void testServerConfigWithValidExpirationValues() {
        // Given
        int sessionExpirationSeconds = 7200;
        int tokenExpirationSeconds = 1800;

        // When
        ServerConfig config = new ServerConfig(
                "localhost", 8080, "Test Server", "data/chat.db", 10, true,
                4096, 256, "239.255.255.250", 9999, true,
                sessionExpirationSeconds, tokenExpirationSeconds
        );

        // Then
        assertThat(config.getSessionExpirationSeconds()).isEqualTo(sessionExpirationSeconds);
        assertThat(config.getTokenExpirationSeconds()).isEqualTo(tokenExpirationSeconds);
    }
}