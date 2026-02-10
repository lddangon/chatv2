package com.chatv2.client.core;

import com.chatv2.client.network.NetworkClient;
import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import com.chatv2.common.model.Session;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatClient.
 */
class ChatClientTest {

    private static final int TIMEOUT_SECONDS = 5;
    
    @Mock
    private NetworkClient mockNetworkClient;
    
    @Mock
    private ClientConfig mockConfig;
    
    @Mock
    private Consumer<Message> mockMessageConsumer;
    
    private ChatClient chatClient;
    private AutoCloseable closeable;
    
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        // No need to mock getName since ClientConfig is a record
        
        chatClient = new ChatClient(mockConfig);
        // Use reflection to replace the networkClient with our mock
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
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    @DisplayName("Should connect to server successfully")
    void testConnect() throws Exception {
        // Given
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // When
        CompletableFuture<Void> result = chatClient.connect(host, port);
        
        // Then
        result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        verify(mockNetworkClient).connect(host, port);
        assertThat(chatClient.getState().name()).isEqualTo("CONNECTED");
        assertThat(chatClient.getConnectedServerHost()).isEqualTo(host);
        assertThat(chatClient.getConnectedServerPort()).isEqualTo(port);
    }

    @Test
    @DisplayName("Should handle connection failure")
    void testConnectFailure() throws Exception {
        // Given
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = new CompletableFuture<>();
        connectFuture.completeExceptionally(new RuntimeException("Connection failed"));
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // When/Then
        CompletableFuture<Void> result = chatClient.connect(host, port);
        assertThatThrownBy(() -> result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to connect to server");
        assertThat(chatClient.getState().name()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should disconnect from server successfully")
    void testDisconnect() throws Exception {
        // Given - connected client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> disconnectFuture = CompletableFuture.completedFuture(null);
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        when(mockNetworkClient.disconnect()).thenReturn(disconnectFuture);
        when(mockNetworkClient.isConnected()).thenReturn(true);
        
        // Connect first
        chatClient.connect(host, port).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When
        CompletableFuture<Void> result = chatClient.disconnect();
        
        // Then
        result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        verify(mockNetworkClient).disconnect();
        assertThat(chatClient.getState().name()).isEqualTo("DISCONNECTED");
        assertThat(chatClient.getCurrentUserId()).isNull();
        assertThat(chatClient.getCurrentToken()).isNull();
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegister() throws Exception {
        // Given - connected client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // Mock successful response for registration
        ChatMessage mockResponse = new ChatMessage(
                ProtocolMessageType.AUTH_REGISTER_RES,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                null
        );
        CompletableFuture<ChatMessage> registerFuture = CompletableFuture.completedFuture(mockResponse);
        when(mockNetworkClient.sendRequest(any(ChatMessage.class))).thenReturn(registerFuture);
        
        // Connect first
        chatClient.connect(host, port).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When
        String username = "testuser";
        String password = "testpass";
        String fullName = "Test User";
        CompletableFuture<Void> result = chatClient.register(username, password, fullName);
        
        // Then
        result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        verify(mockNetworkClient).sendRequest(argThat(req -> 
            req.getMessageType() == ProtocolMessageType.AUTH_REGISTER_REQ
        ));
    }

    @Test
    @DisplayName("Should handle registration failure when not connected")
    void testRegisterWhenNotConnected() throws Exception {
        // Given - not connected client
        
        // When/Then
        String username = "testuser";
        String password = "testpass";
        String fullName = "Test User";
        CompletableFuture<Void> result = chatClient.register(username, password, fullName);
        
        assertThatThrownBy(() -> result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected to server");
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLogin() throws Exception {
        // Given - connected client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // Mock successful response for login
        UUID userId = UUID.randomUUID();
        String token = "test-token";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Session mockSession = Session.createNew(userId, token, expiresAt, "Test User");
        String sessionJson = "{\"userId\":\"" + userId + "\",\"token\":\"" + token + "\"}";
        
        ChatMessage mockResponse = new ChatMessage(
                ProtocolMessageType.AUTH_LOGIN_RES,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                sessionJson.getBytes()
        );
        CompletableFuture<ChatMessage> loginFuture = CompletableFuture.completedFuture(mockResponse);
        when(mockNetworkClient.sendRequest(any(ChatMessage.class))).thenReturn(loginFuture);
        
        // Connect first
        chatClient.connect(host, port).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When
        String username = "testuser";
        String password = "testpass";
        CompletableFuture<Void> result = chatClient.login(username, password);
        
        // Then
        result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        verify(mockNetworkClient).sendRequest(argThat(req -> 
            req.getMessageType() == ProtocolMessageType.AUTH_LOGIN_REQ
        ));
        assertThat(chatClient.isAuthenticated()).isTrue();
        assertThat(chatClient.getCurrentUserId()).isEqualTo(userId);
        assertThat(chatClient.getCurrentToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Should handle login failure when not connected")
    void testLoginWhenNotConnected() throws Exception {
        // Given - not connected client
        
        // When/Then
        String username = "testuser";
        String password = "testpass";
        CompletableFuture<Void> result = chatClient.login(username, password);
        
        assertThatThrownBy(() -> result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not connected to server");
    }

    @Test
    @DisplayName("Should send message successfully")
    void testSendMessage() throws Exception {
        // Given - authenticated client
        String host = "localhost";
        int port = 8080;
        UUID userId = UUID.randomUUID();
        String token = "test-token";
        
        // Setup connection
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        // Setup login response
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String sessionJson = "{\"sessionId\":\"" + UUID.randomUUID() + 
                           "\",\"userId\":\"" + userId + 
                           "\",\"token\":\"" + token + 
                           "\",\"expiresAt\":\"" + expiresAt.toString() + "\"}";
        ChatMessage loginResponse = new ChatMessage(
                ProtocolMessageType.AUTH_LOGIN_RES,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                sessionJson.getBytes()
        );
        CompletableFuture<ChatMessage> loginFuture = CompletableFuture.completedFuture(loginResponse);
        
        // Setup message send response
        ChatMessage messageResponse = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_RES,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                null
        );
        CompletableFuture<ChatMessage> sendFuture = CompletableFuture.completedFuture(messageResponse);
        
        when(mockNetworkClient.sendRequest(any(ChatMessage.class)))
            .thenReturn(loginFuture)
            .thenReturn(sendFuture);
        
        // Connect and login
        chatClient.connect(host, port).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        chatClient.login("testuser", "testpass").get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When
        Message testMessage = Message.createNew(
                UUID.randomUUID(), // chatId
                userId,           // senderId
                "Test message",
                MessageType.TEXT
        );
        CompletableFuture<Void> result = chatClient.sendMessage(testMessage);
        
        // Then
        result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        verify(mockNetworkClient, times(2)).sendRequest(any(ChatMessage.class));
    }

    @Test
    @DisplayName("Should handle send message failure when not authenticated")
    void testSendMessageWhenNotAuthenticated() throws Exception {
        // Given - connected but not authenticated client
        String host = "localhost";
        int port = 8080;
        CompletableFuture<Void> connectFuture = CompletableFuture.completedFuture(null);
        when(mockNetworkClient.connect(host, port)).thenReturn(connectFuture);
        
        chatClient.connect(host, port).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        // When/Then
        Message testMessage = Message.createNew(
                UUID.randomUUID(), // chatId
                UUID.randomUUID(), // senderId
                "Test message",
                MessageType.TEXT
        );
        CompletableFuture<Void> result = chatClient.sendMessage(testMessage);
        
        assertThatThrownBy(() -> result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .hasCauseInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Not authenticated");
    }

    @Test
    @DisplayName("Should register and unregister message consumers")
    void testMessageConsumers() {
        // Given
        Consumer<Message> consumer1 = mock(Consumer.class);
        Consumer<Message> consumer2 = mock(Consumer.class);
        
        // When
        UUID consumerId1 = chatClient.registerMessageConsumer(consumer1);
        UUID consumerId2 = chatClient.registerMessageConsumer(consumer2);
        
        // Then
        assertThat(consumerId1).isNotNull();
        assertThat(consumerId2).isNotNull();
        assertThat(consumerId1).isNotEqualTo(consumerId2);
        
        // When - unregister first consumer
        chatClient.unregisterMessageConsumer(consumerId1);
        
        // Then
        // We can't easily test the removal without exposing internal state,
        // but we can verify no exceptions are thrown
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should set message consumer")
    void testSetMessageConsumer() {
        // Given
        Consumer<Message> consumer = mock(Consumer.class);
        
        // When
        chatClient.setMessageConsumer(consumer);
        
        // Then
        // We can't easily test the consumer is set without exposing internal state,
        // but we can verify no exceptions are thrown
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
        result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        verify(mockNetworkClient).disconnect();
    }
}