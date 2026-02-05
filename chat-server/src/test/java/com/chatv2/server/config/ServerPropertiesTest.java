package com.chatv2.server.config;

import com.chatv2.common.exception.ChatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ServerPropertiesTest {

    @TempDir
    Path tempDir;

    private Path configFile;

    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("test-config.yaml");
    }

    @Test
    void testDefaultConfigCreation() throws IOException {
        // Create a non-existent config file path
        String nonExistentPath = configFile.toString() + "-non-existent";
        
        // This should create default configuration
        ServerProperties properties = ServerProperties.load(nonExistentPath);
        
        // Check default values
        assertEquals("0.0.0.0", properties.getHost());
        assertEquals(8080, properties.getPort());
        assertEquals("ChatV2 Server", properties.getServerName());
        
        // Check UDP config
        assertFalse(properties.getUdpConfig().isEnabled());
        assertEquals("239.255.255.250", properties.getUdpConfig().getMulticastAddress());
        assertEquals(9999, properties.getUdpConfig().getPort());
        assertEquals(5, properties.getUdpConfig().getBroadcastInterval());
        
        // Check database config
        assertEquals("data/chatv2.db", properties.getDatabaseConfig().getPath());
        assertEquals(10, properties.getDatabaseConfig().getConnectionPoolSize());
        
        // Check encryption config
        assertTrue(properties.getEncryptionConfig().isRequired());
        assertEquals("AES-256-GCM", properties.getEncryptionConfig().getDefaultPlugin());
        assertEquals(4096, properties.getEncryptionConfig().getRsaKeySize());
        assertEquals(256, properties.getEncryptionConfig().getAesKeySize());
        
        // Check session config
        assertEquals(3600, properties.getSessionConfig().getTokenExpirationSeconds());
        assertEquals(30, properties.getSessionConfig().getRefreshTokenExpirationDays());
    }

    @Test
    void testCustomConfig() throws IOException {
        // Create a custom config file
        String configContent = """
server:
  host: "192.168.1.100"
  port: 9090
  name: "Test Server"

udp:
  enabled: true
  multicastAddress: "239.255.255.251"
  port: 10000
  broadcastInterval: 10

database:
  path: "/custom/path/db.sqlite"
  connectionPoolSize: 20

encryption:
  required: false
  defaultPlugin: "Custom-Plugin"
  rsaKeySize: 2048
  aesKeySize: 128

session:
  tokenExpirationSeconds: 7200
  refreshTokenExpirationDays: 60
""";
        
        Files.writeString(configFile, configContent);
        
        // Load the custom configuration
        ServerProperties properties = ServerProperties.load(configFile.toString());
        
        // Check custom values
        assertEquals("192.168.1.100", properties.getHost());
        assertEquals(9090, properties.getPort());
        assertEquals("Test Server", properties.getServerName());
        
        // Check UDP config
        assertTrue(properties.getUdpConfig().isEnabled());
        assertEquals("239.255.255.251", properties.getUdpConfig().getMulticastAddress());
        assertEquals(10000, properties.getUdpConfig().getPort());
        assertEquals(10, properties.getUdpConfig().getBroadcastInterval());
        
        // Check database config
        assertEquals("/custom/path/db.sqlite", properties.getDatabaseConfig().getPath());
        assertEquals(20, properties.getDatabaseConfig().getConnectionPoolSize());
        
        // Check encryption config
        assertFalse(properties.getEncryptionConfig().isRequired());
        assertEquals("Custom-Plugin", properties.getEncryptionConfig().getDefaultPlugin());
        assertEquals(2048, properties.getEncryptionConfig().getRsaKeySize());
        assertEquals(128, properties.getEncryptionConfig().getAesKeySize());
        
        // Check session config
        assertEquals(7200, properties.getSessionConfig().getTokenExpirationSeconds());
        assertEquals(60, properties.getSessionConfig().getRefreshTokenExpirationDays());
    }

    @Test
    void testPartialConfigWithDefaults() throws IOException {
        // Create a partial config file with all required sections
        String configContent = """
server:
  host: "custom-host"
  
udp:
  enabled: true

database:
  path: "data/chatv2.db"
  connectionPoolSize: 10

encryption:
  required: true
  defaultPlugin: "AES-256-GCM"
  rsaKeySize: 4096
  aesKeySize: 256

session:
  tokenExpirationSeconds: 1800
""";
        
        Files.writeString(configFile, configContent);
        
        // Load the configuration
        ServerProperties properties = ServerProperties.load(configFile.toString());
        
        // Check custom values
        assertEquals("custom-host", properties.getHost());
        assertEquals(8080, properties.getPort()); // Default
        assertEquals("ChatV2 Server", properties.getServerName()); // Default
        
        // Check UDP config
        assertTrue(properties.getUdpConfig().isEnabled()); // Custom
        assertEquals("239.255.255.250", properties.getUdpConfig().getMulticastAddress()); // Default
        assertEquals(9999, properties.getUdpConfig().getPort()); // Default
        assertEquals(5, properties.getUdpConfig().getBroadcastInterval()); // Default
        
        // Check session config
        assertEquals(1800, properties.getSessionConfig().getTokenExpirationSeconds()); // Custom
        assertEquals(30, properties.getSessionConfig().getRefreshTokenExpirationDays()); // Default
    }

    @Test
    void testInvalidPortValidation() throws IOException {
        // Create a config with invalid port
        String configContent = """
server:
  host: "localhost"
  port: 70000  # Invalid port (> 65535)
  name: "ChatV2 Server"

udp:
  enabled: false
  multicastAddress: "239.255.255.250"
  port: 9999
  broadcastInterval: 5

database:
  path: "data/chatv2.db"
  connectionPoolSize: 10

encryption:
  required: true
  defaultPlugin: "AES-256-GCM"
  rsaKeySize: 4096
  aesKeySize: 256

session:
  tokenExpirationSeconds: 3600
  refreshTokenExpirationDays: 30
""";
        
        Files.writeString(configFile, configContent);
        
        // Load the configuration
        ServerProperties properties = ServerProperties.load(configFile.toString());
        
        // Should use default port
        assertEquals("localhost", properties.getHost());
        assertEquals(8080, properties.getPort()); // Default port due to validation
    }

    @Test
    void testInvalidTokenExpirationValidation() throws IOException {
        // Create a config with invalid token expiration
        String configContent = """
server:
  host: "0.0.0.0"
  port: 8080
  name: "ChatV2 Server"

database:
  path: "data/chatv2.db"
  connectionPoolSize: 10

encryption:
  required: true
  defaultPlugin: "AES-256-GCM"
  rsaKeySize: 4096
  aesKeySize: 256

session:
  tokenExpirationSeconds: 30  # Too short (< 60)
  refreshTokenExpirationDays: 30

udp:
  enabled: false
  multicastAddress: "239.255.255.250"
  port: 9999
  broadcastInterval: 5
""";
        
        Files.writeString(configFile, configContent);
        
        // Load the configuration
        ServerProperties properties = ServerProperties.load(configFile.toString());
        
        // Should use default token expiration
        assertEquals(3600, properties.getSessionConfig().getTokenExpirationSeconds()); // Default due to validation
    }
}