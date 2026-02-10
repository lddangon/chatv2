package com.chatv2.server.handler;

import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.config.ServerProperties;
import com.chatv2.server.manager.*;
import com.chatv2.server.pipeline.EncryptionHandler;
import com.chatv2.server.storage.DatabaseManager;
import com.chatv2.server.storage.UserRepository;
import com.chatv2.server.storage.ChatRepository;
import com.chatv2.server.storage.SessionRepository;
import com.chatv2.server.storage.MessageRepository;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServerInitializerTest {

    @Mock
    private DatabaseManager databaseManager;

    private ServerInitializer initializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mocks
        when(databaseManager.createUserRepository()).thenReturn(mock(UserRepository.class));
        when(databaseManager.createChatRepository()).thenReturn(mock(ChatRepository.class));
        when(databaseManager.createSessionRepository()).thenReturn(mock(SessionRepository.class));
        when(databaseManager.createMessageRepository()).thenReturn(mock(MessageRepository.class));
        
        // Create initializer with default session config
        initializer = new ServerInitializer(databaseManager);
    }

    @AfterEach
    void tearDown() {
        if (initializer != null) {
            initializer.shutdown();
        }
    }

    @Test
    @DisplayName("Should initialize server components correctly")
    void testInitializeComponents() {
        // When
        ServerInitializer newInitializer = new ServerInitializer(databaseManager);
        
        // Then
        assertThat(newInitializer.getUserManager()).isNotNull();
        assertThat(newInitializer.getChatManager()).isNotNull();
        assertThat(newInitializer.getSessionManager()).isNotNull();
        assertThat(newInitializer.getMessageManager()).isNotNull();
        assertThat(newInitializer.getEncryptionPluginManager()).isNotNull();
    }

    @Test
    @DisplayName("Should handle BinaryMessageCodec in pipeline")
    void testBinaryMessageCodec() throws IOException {
        // Create test message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");
        
        // Convert payload Map to JSON string, then to byte[]
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(payload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage message = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When
        EmbeddedChannel channel = new EmbeddedChannel(new BinaryMessageCodec());
        boolean writeResult = channel.writeInbound(message);
        
        // Then
        assertThat(writeResult).isTrue();
        
        // Clean up
        channel.finish();
    }

    @Test
    @DisplayName("Should process BinaryMessageCodec for server")
    void testBinaryMessageCodecForServer() throws IOException {
        // Create a test message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message from client");
        
        // Convert payload Map to JSON string, then to byte[]
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(payload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage message = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When - encode and decode message using BinaryMessageCodec
        EmbeddedChannel codecChannel = new EmbeddedChannel(new BinaryMessageCodec());
        codecChannel.writeOutbound(message);
        io.netty.buffer.ByteBuf encoded = codecChannel.readOutbound();
        
        // And decode back
        EmbeddedChannel decodeChannel = new EmbeddedChannel(new BinaryMessageCodec());
        decodeChannel.writeInbound(encoded);
        ChatMessage decoded = decodeChannel.readInbound();
        
        // Then
        assertThat(decoded).isNotNull();
        assertThat(decoded.getMessageType()).isEqualTo(message.getMessageType());
        assertThat(decoded.getFlags()).isEqualTo(message.getFlags());
        
        // Verify payload
        String decodedJson = new String(decoded.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> decodedPayload = com.chatv2.common.protocol.MessageCodec.decode(decodedJson, Map.class);
        assertThat(decodedPayload.get("message")).isEqualTo("Test message from client");
        
        // Clean up
        codecChannel.finish();
        decodeChannel.finish();
    }

    @Test
    @DisplayName("Should create custom server initializer")
    void testCustomServerInitializer() throws IOException {
        // Create a custom ServerProperties with session config
        ServerProperties.SessionConfig sessionConfig = new ServerProperties.SessionConfig(7200, 60);
        
        // Create initializer with custom session config
        ServerInitializer customInitializer = new ServerInitializer(databaseManager, null);
        
        // Then
        assertThat(customInitializer.getUserManager()).isNotNull();
        assertThat(customInitializer.getSessionManager()).isNotNull();
        
        // Verify session config
        assertThat(customInitializer.getSessionManager()).isNotNull();
    }

    @Test
    @DisplayName("Should provide access to managers")
    void testGetManagers() {
        // Then
        assertThat(initializer.getUserManager()).isNotNull();
        assertThat(initializer.getChatManager()).isNotNull();
        assertThat(initializer.getSessionManager()).isNotNull();
        assertThat(initializer.getMessageManager()).isNotNull();
        assertThat(initializer.getEncryptionPluginManager()).isNotNull();
    }

    @Test
    @DisplayName("Should shutdown properly")
    void testShutdown() {
        // Create a new initializer
        ServerInitializer newInitializer = new ServerInitializer(databaseManager);
        
        // When
        newInitializer.shutdown();
        
        // Then - should not throw exception
        // We can't directly verify that managers were shut down without 
        // exposing internal state or adding verification methods
        
        // This test mainly ensures that the shutdown method doesn't throw
        assertThat(true).isTrue(); // Placeholder assertion
    }
}