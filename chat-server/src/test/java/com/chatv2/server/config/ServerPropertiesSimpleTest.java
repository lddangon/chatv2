package com.chatv2.server.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerPropertiesSimpleTest {
    public static void main(String[] args) {
        try {
            // Test 1: Default configuration creation
            System.out.println("Test 1: Default configuration creation");
            String nonExistentPath = "/tmp/non-existent-config.yaml";
            ServerProperties defaultProps = ServerProperties.load(nonExistentPath);
            
            assert "0.0.0.0".equals(defaultProps.getHost()) : "Default host should be 0.0.0.0";
            assert 8080 == defaultProps.getPort() : "Default port should be 8080";
            assert "ChatV2 Server".equals(defaultProps.getServerName()) : "Default name should be 'ChatV2 Server'";
            
            assert !defaultProps.getUdpConfig().isEnabled() : "UDP should be disabled by default";
            assert "239.255.255.250".equals(defaultProps.getUdpConfig().getMulticastAddress()) : "Default multicast address incorrect";
            assert 9999 == defaultProps.getUdpConfig().getPort() : "Default UDP port should be 9999";
            assert 5 == defaultProps.getUdpConfig().getBroadcastInterval() : "Default broadcast interval should be 5";
            
            assert "data/chatv2.db".equals(defaultProps.getDatabaseConfig().getPath()) : "Default DB path incorrect";
            assert 10 == defaultProps.getDatabaseConfig().getConnectionPoolSize() : "Default pool size should be 10";
            
            assert defaultProps.getEncryptionConfig().isRequired() : "Encryption should be required by default";
            assert "AES-256-GCM".equals(defaultProps.getEncryptionConfig().getDefaultPlugin()) : "Default plugin incorrect";
            assert 4096 == defaultProps.getEncryptionConfig().getRsaKeySize() : "Default RSA key size should be 4096";
            assert 256 == defaultProps.getEncryptionConfig().getAesKeySize() : "Default AES key size should be 256";
            
            assert 3600 == defaultProps.getSessionConfig().getTokenExpirationSeconds() : "Default token expiration should be 3600";
            assert 30 == defaultProps.getSessionConfig().getRefreshTokenExpirationDays() : "Default refresh days should be 30";
            
            System.out.println("✓ Default configuration test passed");
            
            // Test 2: Custom configuration
            System.out.println("\nTest 2: Custom configuration");
            Path tempDir = Files.createTempDirectory("server-properties-test");
            Path configFile = tempDir.resolve("test-config.yaml");
            
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
            
            ServerProperties customProps = ServerProperties.load(configFile.toString());
            
            assert "192.168.1.100".equals(customProps.getHost()) : "Custom host incorrect";
            assert 9090 == customProps.getPort() : "Custom port incorrect";
            assert "Test Server".equals(customProps.getServerName()) : "Custom name incorrect";
            
            assert customProps.getUdpConfig().isEnabled() : "UDP should be enabled";
            assert "239.255.255.251".equals(customProps.getUdpConfig().getMulticastAddress()) : "Custom multicast address incorrect";
            assert 10000 == customProps.getUdpConfig().getPort() : "Custom UDP port incorrect";
            assert 10 == customProps.getUdpConfig().getBroadcastInterval() : "Custom broadcast interval incorrect";
            
            assert "/custom/path/db.sqlite".equals(customProps.getDatabaseConfig().getPath()) : "Custom DB path incorrect";
            assert 20 == customProps.getDatabaseConfig().getConnectionPoolSize() : "Custom pool size incorrect";
            
            assert !customProps.getEncryptionConfig().isRequired() : "Encryption should be disabled";
            assert "Custom-Plugin".equals(customProps.getEncryptionConfig().getDefaultPlugin()) : "Custom plugin incorrect";
            assert 2048 == customProps.getEncryptionConfig().getRsaKeySize() : "Custom RSA key size incorrect";
            assert 128 == customProps.getEncryptionConfig().getAesKeySize() : "Custom AES key size incorrect";
            
            assert 7200 == customProps.getSessionConfig().getTokenExpirationSeconds() : "Custom token expiration incorrect";
            assert 60 == customProps.getSessionConfig().getRefreshTokenExpirationDays() : "Custom refresh days incorrect";
            
            System.out.println("✓ Custom configuration test passed");
            
            // Test 3: Invalid values validation
            System.out.println("\nTest 3: Invalid values validation");
            Path invalidConfigFile = tempDir.resolve("invalid-config.yaml");
            
            String invalidConfigContent = """
server:
  host: "localhost"
  port: 70000  # Invalid port (> 65535)

session:
  tokenExpirationSeconds: 30  # Too short (< 60)
""";
            
            Files.writeString(invalidConfigFile, invalidConfigContent);
            
            ServerProperties validatedProps = ServerProperties.load(invalidConfigFile.toString());
            
            assert "localhost".equals(validatedProps.getHost()) : "Host should be preserved";
            assert 8080 == validatedProps.getPort() : "Port should be reset to default";
            assert 3600 == validatedProps.getSessionConfig().getTokenExpirationSeconds() : "Token expiration should be reset to default";
            
            System.out.println("✓ Invalid values validation test passed");
            
            System.out.println("\n✅ All tests passed! ServerProperties conversion from records to classes was successful.");
            
        } catch (IOException e) {
            System.err.println("Test failed with IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (AssertionError e) {
            System.err.println("Test failed with assertion error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Test failed with unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}