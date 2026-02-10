package com.chatv2.client.core;

import com.chatv2.client.network.NetworkClient;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatClient.
 * Tests the basic functionality without requiring network communication.
 */
class ChatClientUnitTest {

    private ChatClient chatClient;
    private NetworkClient mockNetworkClient;
    
    @BeforeEach
    void setUp() {
        // Create mocks first
        mockNetworkClient = mock(NetworkClient.class);
        MockitoAnnotations.openMocks(this);
        
        // Create a client with default configuration
        ClientConfig config = new ClientConfig();
        chatClient = new ChatClient(config);
        
        // Use reflection to inject mock NetworkClient
        try {
            java.lang.reflect.Field networkClientField = ChatClient.class.getDeclaredField("networkClient");
            networkClientField.setAccessible(true);
            networkClientField.set(chatClient, mockNetworkClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock NetworkClient", e);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (chatClient != null) {
            chatClient.shutdown().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should initialize with default configuration")
    void testClientInitialization() {
        // Given - Client created with default configuration
        
        // Then - Client should be created
        assertThat(chatClient).isNotNull();
        assertThat(chatClient.getState()).isEqualTo(ConnectionState.DISCONNECTED);
        assertThat(chatClient.getCurrentUserId()).isNull();
        assertThat(chatClient.getCurrentToken()).isNull();
        assertThat(chatClient.getConnectedServerHost()).isNull();
        assertThat(chatClient.getConnectedServerPort()).isEqualTo(0);
        assertThat(chatClient.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Should connect to server successfully")
    void testConnectToServer() throws Exception {
        // Given
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // When
        CompletableFuture<Void> result = chatClient.connect(host, port);
        
        // Then
        result.get(5, TimeUnit.SECONDS);
        assertThat(chatClient.getState()).isEqualTo(ConnectionState.CONNECTED);
        assertThat(chatClient.getConnectedServerHost()).isEqualTo(host);
        assertThat(chatClient.getConnectedServerPort()).isEqualTo(port);
    }

    @Test
    @DisplayName("Should handle connection failure")
    void testConnectionFailure() throws Exception {
        // Given
        String host = "localhost";
        int port = 8080;
        RuntimeException exception = new RuntimeException("Connection failed");
        CompletableFuture<Void> connectFuture = new CompletableFuture<>();
        connectFuture.completeExceptionally(exception);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // When
        CompletableFuture<Void> result = chatClient.connect(host, port);
        
        // Then
        try {
            result.get(5, TimeUnit.SECONDS);
            // Should not reach here
            assertThat(false).isTrue();
        } catch (Exception e) {
            assertThat(e.getCause()).isEqualTo(exception);
            assertThat(chatClient.getState()).isEqualTo(ConnectionState.ERROR);
        }
    }

    @Test
    @DisplayName("Should disconnect from server successfully")
    void testDisconnectFromServer() throws Exception {
        // Given - Connected client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> disconnectFuture = CompletableFuture.completedFuture(null);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        when(mockNetworkClient.disconnect()).thenReturn(disconnectFuture);
        when(mockNetworkClient.isConnected()).thenReturn(true);
        
        // Connect first
        chatClient.connect(host, port).get(5, TimeUnit.SECONDS);
        
        // When
        CompletableFuture<Void> result = chatClient.disconnect();
        
        // Then
        result.get(5, TimeUnit.SECONDS);
        assertThat(chatClient.getState()).isEqualTo(ConnectionState.DISCONNECTED);
        assertThat(chatClient.getCurrentUserId()).isNull();
        assertThat(chatClient.getCurrentToken()).isNull();
        assertThat(chatClient.getConnectedServerHost()).isNull();
        assertThat(chatClient.getConnectedServerPort()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should register user successfully")
    void testUserRegistration() throws Exception {
        // Given - Connected client
        String host = "localhost";
        int port = 8080;
        String username = "testuser";
        String password = "testpass";
        String fullName = "Test User";
        
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        ChatMessage responseMessage = new ChatMessage(
                ProtocolMessageType.AUTH_REGISTER_RES,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                null
        );
        CompletableFuture<ChatMessage> registerFuture = CompletableFuture.completedFuture(responseMessage);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        when(mockNetworkClient.sendRequest(any(ChatMessage.class))).thenReturn(registerFuture);
        when(mockNetworkClient.isConnected()).thenReturn(true);
        
        // Connect first
        chatClient.connect(host, port).get(5, TimeUnit.SECONDS);
        
        // When
        CompletableFuture<Void> result = chatClient.register(username, password, fullName);
        
        // Then
        result.get(5, TimeUnit.SECONDS);
        assertThat(chatClient.getState()).isEqualTo(ConnectionState.CONNECTED);
    }

    @Test
    @DisplayName("Should handle registration failure when not connected")
    void testRegistrationWhenNotConnected() throws Exception {
        // Given - Not connected client
        String username = "testuser";
        String password = "testpass";
        String fullName = "Test User";
        
        when(mockNetworkClient.isConnected()).thenReturn(false);
        
        // When
        CompletableFuture<Void> result = chatClient.register(username, password, fullName);
        
        // Then
        try {
            result.get(5, TimeUnit.SECONDS);
            // Should not reach here
            assertThat(false).isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Not connected to server");
            assertThat(chatClient.getState()).isEqualTo(ConnectionState.DISCONNECTED);
        }
    }

    @Test
    @DisplayName("Should login user successfully")
    void testUserLogin() throws Exception {
        // Given - Connected client
        String host = "localhost";
        int port = 8080;
        String username = "testuser";
        String password = "testpass";
        UUID userId = UUID.randomUUID();
        String token = "test-token";
        
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        
        // Create a session JSON response
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", UUID.randomUUID());
        sessionData.put("userId", userId.toString());
        sessionData.put("token", token);
        sessionData.put("expiresAt", "2026-02-09T21:00:00Z");
        sessionData.put("createdAt", "2026-02-09T21:00:00Z");
        sessionData.put("lastAccessedAt", "2026-02-09T21:00:00Z");
        sessionData.put("deviceInfo", "Test Device");
        
        String sessionJson = "{\"sessionId\":\"" + sessionData.get("sessionId") + 
                           "\",\"userId\":\"" + sessionData.get("userId") + 
                           "\",\"token\":\"" + sessionData.get("token") + "\"}";
        
        ChatMessage responseMessage = new ChatMessage(
                ProtocolMessageType.AUTH_LOGIN_RES,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                sessionJson.getBytes(StandardCharsets.UTF_8)
        );
        CompletableFuture<ChatMessage> loginFuture = CompletableFuture.completedFuture(responseMessage);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        when(mockNetworkClient.sendRequest(any(ChatMessage.class))).thenReturn(loginFuture);
        when(mockNetworkClient.isConnected()).thenReturn(true);
        
        // Connect first
        chatClient.connect(host, port).get(5, TimeUnit.SECONDS);
        
        // When
        CompletableFuture<Void> result = chatClient.login(username, password);
        
        // Then
        result.get(5, TimeUnit.SECONDS);
        assertThat(chatClient.getState()).isEqualTo(ConnectionState.AUTHENTICATED);
        assertThat(chatClient.getCurrentUserId()).isEqualTo(userId);
        assertThat(chatClient.getCurrentToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should handle login failure when not connected")
    void testLoginWhenNotConnected() throws Exception {
        // Given - Not connected client
        String username = "testuser";
        String password = "testpass";
        
        when(mockNetworkClient.isConnected()).thenReturn(false);
        
        // When
        CompletableFuture<Void> result = chatClient.login(username, password);
        
        // Then
        try {
            result.get(5, TimeUnit.SECONDS);
            // Should not reach here
            assertThat(false).isTrue();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Not connected to server");
            assertThat(chatClient.getState()).isEqualTo(ConnectionState.DISCONNECTED);
        }
    }

    @Test
    @DisplayName("Should set message consumer")
    void testSetMessageConsumer() throws Exception {
        // Given - Connected client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        when(mockNetworkClient.isConnected()).thenReturn(true);
        
        // Connect first
        chatClient.connect(host, port).get(5, TimeUnit.SECONDS);
        
        // When
        java.util.function.Consumer<com.chatv2.common.model.Message> consumer = 
            message -> assertThat(message).isNotNull();
        
        chatClient.setMessageConsumer(consumer);
        
        // Then - Verify the consumer was set
        // This is a simple test to ensure no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should register message consumer")
    void testRegisterMessageConsumer() throws Exception {
        // Given - Connected client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        when(mockNetworkClient.isConnected()).thenReturn(true);
        
        // Connect first
        chatClient.connect(host, port).get(5, TimeUnit.SECONDS);
        
        // When
        java.util.function.Consumer<com.chatv2.common.model.Message> consumer = 
            message -> assertThat(message).isNotNull();
        
        UUID consumerId = chatClient.registerMessageConsumer(consumer);
        
        // Then - Verify the consumer was registered
        assertThat(consumerId).isNotNull();
        
        // When - Unregister consumer
        chatClient.unregisterMessageConsumer(consumerId);
        
        // Then - No assertions needed, just ensure no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should shutdown properly")
    void testShutdown() throws Exception {
        // Given
        CompletableFuture<Void> disconnectFuture = CompletableFuture.completedFuture(null);
        
        when(mockNetworkClient.disconnect()).thenReturn(disconnectFuture);
        
        // When
        CompletableFuture<Void> result = chatClient.shutdown();
        
        // Then
        result.get(5, TimeUnit.SECONDS);
        
        // Verify network client was called
        org.mockito.Mockito.verify(mockNetworkClient).disconnect();
    }
}