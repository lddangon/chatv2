package com.chatv2.client.network;

import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for BinaryMessageCodec using embedded channels.
 * Tests encoding and decoding without network communication.
 */
class BinaryMessageCodecIntegrationTest {

    private EmbeddedChannel serverChannel;
    private EmbeddedChannel clientChannel;

    @BeforeEach
    void setUp() {
        // Create server pipeline with BinaryMessageCodec
        serverChannel = new EmbeddedChannel(new BinaryMessageCodec());
        
        // Create client pipeline with BinaryMessageCodec
        clientChannel = new EmbeddedChannel(new BinaryMessageCodec());
    }

    @AfterEach
    void tearDown() {
        if (serverChannel != null) {
            serverChannel.finish();
        }
        if (clientChannel != null) {
            clientChannel.finish();
        }
    }

    @Test
    @DisplayName("Should encode and decode message correctly")
    void testEncodeDecodeMessage() throws Exception {
        // Given - Create a message on client side
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Hello from client");
        payload.put("sender", "clientUser");
        
        // Convert payload Map to JSON string, then to byte[]
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(payload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage clientMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When - Send from client to server
        clientChannel.writeOutbound(clientMessage);
        io.netty.buffer.ByteBuf encodedMessage = clientChannel.readOutbound();
        
        // Then - Verify encoded message is valid
        assertThat(encodedMessage).isNotNull();
        assertThat(encodedMessage.readableBytes()).isGreaterThan(0);
        
        // When - Send encoded message to server
        serverChannel.writeInbound(encodedMessage);
        ChatMessage serverMessage = serverChannel.readInbound();
        
        // Then - Verify server received the correct message
        assertThat(serverMessage).isNotNull();
        assertThat(serverMessage.getMessageType()).isEqualTo(clientMessage.getMessageType());
        assertThat(serverMessage.getFlags()).isEqualTo(clientMessage.getFlags());
        
        // Convert payload bytes back to string for verification
        String receivedJson = new String(serverMessage.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> receivedPayload = com.chatv2.common.protocol.MessageCodec.decode(receivedJson, Map.class);
        assertThat(receivedPayload.get("message")).isEqualTo("Hello from client");
        assertThat(receivedPayload.get("sender")).isEqualTo("clientUser");
    }

    @Test
    @DisplayName("Should handle messages with different flags")
    void testMessagesWithFlags() throws Exception {
        // Given - Create messages with different flags
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("message", "Test message");
        
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(messagePayload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
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
        
        // When - Send messages
        clientChannel.writeOutbound(encryptedMessage);
        io.netty.buffer.ByteBuf encryptedEncoded = clientChannel.readOutbound();
        
        clientChannel.writeOutbound(urgentMessage);
        io.netty.buffer.ByteBuf urgentEncoded = clientChannel.readOutbound();
        
        // Send to server
        serverChannel.writeInbound(encryptedEncoded);
        serverChannel.writeInbound(urgentEncoded);
        
        // Then - Verify server received messages with correct flags
        ChatMessage receivedEncrypted = serverChannel.readInbound();
        ChatMessage receivedUrgent = serverChannel.readInbound();
        
        assertThat(receivedEncrypted).isNotNull();
        assertThat(receivedEncrypted.isEncrypted()).isTrue();
        assertThat(receivedEncrypted.isUrgent()).isFalse();
        
        assertThat(receivedUrgent).isNotNull();
        assertThat(receivedUrgent.isEncrypted()).isFalse();
        assertThat(receivedUrgent.isUrgent()).isTrue();
    }

    @Test
    @DisplayName("Should handle messages with empty payload")
    void testEmptyPayload() {
        // Given - Create a message with empty payload
        ChatMessage message = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                new byte[0]
        );
        
        // When - Send from client to server
        clientChannel.writeOutbound(message);
        io.netty.buffer.ByteBuf encodedMessage = clientChannel.readOutbound();
        
        // Then - Verify encoded message is valid
        assertThat(encodedMessage).isNotNull();
        assertThat(encodedMessage.readableBytes()).isGreaterThan(0);
        
        // When - Send encoded message to server
        serverChannel.writeInbound(encodedMessage);
        ChatMessage serverMessage = serverChannel.readInbound();
        
        // Then - Verify server received the correct message
        assertThat(serverMessage).isNotNull();
        assertThat(serverMessage.getMessageType()).isEqualTo(message.getMessageType());
        assertThat(serverMessage.getFlags()).isEqualTo(message.getFlags());
        assertThat(serverMessage.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("Should handle round trip message exchange")
    void testRoundTripMessageExchange() throws Exception {
        // Given - Create a message on server side
        Map<String, Object> serverPayload = new HashMap<>();
        serverPayload.put("message", "Hello from server");
        serverPayload.put("sender", "serverUser");
        
        String jsonPayload = com.chatv2.common.protocol.MessageCodec.encode(serverPayload);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage serverMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_RECEIVE,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When - Send from server to client
        serverChannel.writeOutbound(serverMessage);
        io.netty.buffer.ByteBuf encodedMessage = serverChannel.readOutbound();
        
        // Then - Verify encoded message is valid
        assertThat(encodedMessage).isNotNull();
        assertThat(encodedMessage.readableBytes()).isGreaterThan(0);
        
        // When - Send encoded message to client
        clientChannel.writeInbound(encodedMessage);
        ChatMessage clientMessage = clientChannel.readInbound();
        
        // Then - Verify client received the correct message
        assertThat(clientMessage).isNotNull();
        assertThat(clientMessage.getMessageType()).isEqualTo(serverMessage.getMessageType());
        assertThat(clientMessage.getFlags()).isEqualTo(serverMessage.getFlags());
        
        // Convert payload bytes back to string for verification
        String receivedJson = new String(clientMessage.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> receivedPayload = com.chatv2.common.protocol.MessageCodec.decode(receivedJson, Map.class);
        assertThat(receivedPayload.get("message")).isEqualTo("Hello from server");
        assertThat(receivedPayload.get("sender")).isEqualTo("serverUser");
    }
}