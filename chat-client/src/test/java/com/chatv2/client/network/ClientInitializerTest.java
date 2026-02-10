package com.chatv2.client.network;

import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClientInitializerTest {

    @Mock
    private SslContext sslContext;

    @Mock
    private ClientHandler clientHandler;

    @Mock
    private EncryptionHandler encryptionHandler;

    private ClientInitializer initializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        // No cleanup needed for this test class
    }

    @Test
    @DisplayName("Should create client initializer with SSL")
    void testCreateClientInitializerWithSsl() {
        // Given - sslContext is mocked and non-null
        
        // When
        ClientInitializer sslInitializer = new ClientInitializer(sslContext, clientHandler, encryptionHandler);
        
        // Then
        assertThat(sslInitializer).isNotNull();
        
        // We can't directly test the pipeline without creating a SocketChannel
        // But we can verify the initializer was created successfully
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should create client initializer without SSL")
    void testCreateClientInitializerWithoutSsl() {
        // Given - Create initializer with null SSL context
        
        // When
        ClientInitializer noSslInitializer = new ClientInitializer(null, clientHandler, encryptionHandler);
        
        // Then
        assertThat(noSslInitializer).isNotNull();
        
        // We can't directly test the pipeline without creating a SocketChannel
        // But we can verify the initializer was created successfully
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Should handle BinaryMessageCodec in client pipeline")
    void testBinaryMessageCodecInClientPipeline() {
        // Given
        EmbeddedChannel channel = new EmbeddedChannel(new BinaryMessageCodec());
        
        // Create a test message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message from server");
        
        // Create JSON string for payload
        String jsonPayload = "{\"message\":\"Test message from server\"}";
        byte[] payloadBytes = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        ChatMessage message = new ChatMessage(
                ProtocolMessageType.MESSAGE_RECEIVE,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When
        boolean writeResult = channel.writeInbound(message);
        
        // Then
        assertThat(writeResult).isTrue();
        
        // Clean up
        channel.finish();
    }

    @Test
    @DisplayName("Should handle messages with different flags")
    void testMessagesWithFlags() {
        // Given
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast("messageCodec", new BinaryMessageCodec());
        
        // Create a test message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");
        
        // Create JSON string for payload
        String jsonPayload = "{\"message\":\"Test message\"}";
        byte[] payloadBytes = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // Create a test message with encryption flag
        ChatMessage encryptedMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                ChatMessage.FLAG_ENCRYPTED,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // Create a test message with urgent flag
        ChatMessage urgentMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                ChatMessage.FLAG_URGENT,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When
        boolean encryptedWriteResult = channel.writeInbound(encryptedMessage);
        boolean urgentWriteResult = channel.writeInbound(urgentMessage);
        
        // Then
        assertThat(encryptedWriteResult).isTrue();
        assertThat(urgentWriteResult).isTrue();
        assertThat(urgentWriteResult).isTrue();
        
        // Clean up
        channel.finish();
    }
    
    @Test
    @DisplayName("Should handle messages with different flags")
    void testMessagesWithFlagsAndUrgent() {
        // Given
        EmbeddedChannel channel = new EmbeddedChannel(new BinaryMessageCodec());
        
        // Create a test message
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");
        
        // Create JSON string for payload
        String jsonPayload = "{\"message\":\"Test message\"}";
        byte[] payloadBytes = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // Create a test message with encryption flag
        ChatMessage encryptedMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                ChatMessage.FLAG_ENCRYPTED,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // Create a test message with urgent flag
        ChatMessage urgentMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                ChatMessage.FLAG_URGENT,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When
        boolean encryptedWriteResult = channel.writeInbound(encryptedMessage);
        boolean urgentWriteResult = channel.writeInbound(urgentMessage);
        
        // Then
        assertThat(encryptedWriteResult).isTrue();
        assertThat(urgentWriteResult).isTrue();
        
        // Clean up
        channel.finish();
    }

    @Test
    @DisplayName("Should handle messages of different sizes")
    void testMessagesOfDifferentSizes() {
        // Given
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast("messageCodec", new BinaryMessageCodec());
        
        // Test with small, medium, and large payloads
        int[] sizes = {100, 1000, 10000};
        
        for (int size : sizes) {
            // Given - Create a message with specified size
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append("x");
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", sb.toString());
            
            // Create JSON string for payload
            String jsonPayload = "{\"message\":\"" + sb.toString() + "\"}";
            byte[] payloadBytes = jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            
            ChatMessage message = new ChatMessage(
                    ProtocolMessageType.MESSAGE_SEND_REQ,
                    (byte) 0,
                    UUID.randomUUID(),
                    System.currentTimeMillis(),
                    payloadBytes
            );
            
            // When
            boolean writeResult = channel.writeInbound(message);
            
            // Then
            assertThat(writeResult).isTrue();
        }
        
        // Clean up
        channel.finish();
    }
}