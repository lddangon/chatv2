package com.chatv2.client.integration;

import com.chatv2.client.core.ChatClient;
import com.chatv2.client.core.ClientConfig;
import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import com.chatv2.launcher.server.ServerLauncher;
import com.chatv2.server.core.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for client-server connection using ServerLauncher.
 */
class ClientServerIntegrationTest {

    private static final int TEST_PORT = 18080;
    private static final String TEST_HOST = "localhost";
    private static final int TIMEOUT_SECONDS = 10;
    private ChatClient client;
    private String testDbPath;

    @BeforeEach
    void setUp() throws Exception {
        // Prepare test database path
        testDbPath = "./test_integration_" + System.currentTimeMillis() + ".db";
        
        // Create server configuration for testing
        ServerConfig config = new ServerConfig(
            TEST_HOST,
            TEST_PORT,
            "Test Server",
            testDbPath,
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
        
        // Start server using ServerLauncher's public test method
        CompletableFuture<Void> serverStart = ServerLauncher.startTestServer(config);
        serverStart.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Create client with ClientConfig
        ClientConfig clientConfig = new ClientConfig();
        client = new ChatClient(clientConfig);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up client
        if (client != null) {
            client.disconnect().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        
        // Stop server using ServerLauncher's public test method
        try {
            CompletableFuture<Void> serverStop = ServerLauncher.stopTestServer();
            serverStop.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("Error stopping server: " + e.getMessage());
        }
        
        // Clean up test database
        File dbFile = new File(testDbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    @DisplayName("Integration test for complete client-server workflow")
    void testClientServerWorkflow() throws Exception {
        // Connect to server on port 18080
        client.connect(TEST_HOST, TEST_PORT).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Register a new user
        String username = "testuser_" + System.currentTimeMillis();
        String password = "testpass";
        String fullName = "Test User";
        
        client.register(username, password, fullName).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Login with the registered user
        client.login(username, password).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // Verify authentication status
        assertTrue(client.isAuthenticated(), "Client should be authenticated after login");
        
        // Send a test message
        Message testMessage = Message.createNew(
                UUID.randomUUID(), // chatId
                client.getCurrentUserId(), // senderId
                "Hello, this is a test message!",
                MessageType.TEXT
        );
        
        client.sendMessage(testMessage).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // If we reach here without exceptions, the test passes
        assertTrue(true, "All operations completed successfully");
    }
}