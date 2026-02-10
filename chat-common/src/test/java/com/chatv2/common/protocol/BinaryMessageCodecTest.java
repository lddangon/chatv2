package com.chatv2.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryMessageCodecTest {

    private BinaryMessageCodec codec;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        codec = new BinaryMessageCodec();
        channel = new EmbeddedChannel(codec);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finish();
        }
    }

    @Test
    @DisplayName("Should encode and decode ChatMessage correctly")
    void testEncodeDecodeMessage() throws IOException {
        // Given
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("message", "Hello, world!");
        payloadMap.put("sender", "user1");
        
        // Convert payload Map to JSON string, then to byte[]
        String jsonPayload = MessageCodec.encode(payloadMap);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage originalMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );

        // When - Encode message using channel
        channel.writeOutbound(originalMessage);
        ByteBuf encodedMessage = channel.readOutbound();
        
        // And decode it back
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();

        // Then
        assertThat(decodedMessage).isNotNull();
        assertThat(decodedMessage.getMessageType()).isEqualTo(originalMessage.getMessageType());
        assertThat(decodedMessage.getFlags()).isEqualTo(originalMessage.getFlags());
        
        // Verify payload content
        String decodedJson = new String(decodedMessage.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> decodedPayloadMap = MessageCodec.decode(decodedJson, Map.class);
        assertThat(decodedPayloadMap.get("message")).isEqualTo("Hello, world!");
        assertThat(decodedPayloadMap.get("sender")).isEqualTo("user1");
    }

    @Test
    @DisplayName("Should handle empty payload correctly")
    void testEncodeDecodeEmptyPayload() {
        // Given
        ChatMessage originalMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                new byte[0]
        );

        // When - Encode message using channel
        channel.writeOutbound(originalMessage);
        ByteBuf encodedMessage = channel.readOutbound();
        
        // And decode it back
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();

        // Then
        assertThat(decodedMessage).isNotNull();
        assertThat(decodedMessage.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null payload correctly")
    void testEncodeDecodeNullPayload() {
        // Given
        ChatMessage originalMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                null
        );

        // When - Encode message using channel
        channel.writeOutbound(originalMessage);
        ByteBuf encodedMessage = channel.readOutbound();
        
        // And decode it back
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();

        // Then
        assertThat(decodedMessage).isNotNull();
        assertThat(decodedMessage.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("Should handle encrypted messages")
    void testEncryptedMessages() throws IOException {
        // Given
        byte[] encryptedPayload = "encrypted data".getBytes(StandardCharsets.UTF_8);
        
        ChatMessage encryptedMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                ChatMessage.FLAG_ENCRYPTED,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                encryptedPayload
        );

        // When - Encode message using channel
        channel.writeOutbound(encryptedMessage);
        ByteBuf encodedMessage = channel.readOutbound();
        
        // And decode it back
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();

        // Then
        assertThat(decodedMessage).isNotNull();
        assertThat(decodedMessage.isEncrypted()).isTrue();
        assertThat(new String(decodedMessage.getPayload(), StandardCharsets.UTF_8))
                .isEqualTo("encrypted data");
    }

    @Test
    @DisplayName("Should handle large payloads")
    void testLargePayloads() throws IOException {
        // Given
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("x");
        }
        
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("largeMessage", sb.toString());
        
        // Convert payload Map to JSON string, then to byte[]
        String jsonPayload = MessageCodec.encode(payloadMap);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        ChatMessage originalMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );

        // When - Encode message using channel
        channel.writeOutbound(originalMessage);
        ByteBuf encodedMessage = channel.readOutbound();
        
        // And decode it back
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();

        // Then
        assertThat(decodedMessage).isNotNull();
        
        String decodedJson = new String(decodedMessage.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> decodedPayloadMap = MessageCodec.decode(decodedJson, Map.class);
        assertThat(((String) decodedPayloadMap.get("largeMessage")).length()).isEqualTo(10000);
    }

    @Test
    @DisplayName("Should handle multiple flags correctly")
    void testMultipleFlags() throws IOException {
        // Given
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("message", "Message with multiple flags");
        
        // Convert payload Map to JSON string, then to byte[]
        String jsonPayload = MessageCodec.encode(payloadMap);
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        
        // Create message with multiple flags
        byte multipleFlags = (byte) (ChatMessage.FLAG_ENCRYPTED | ChatMessage.FLAG_URGENT | ChatMessage.FLAG_ACK_REQUIRED);
        ChatMessage messageWithFlags = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                multipleFlags,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );
        
        // When - Encode and decode message
        channel.writeOutbound(messageWithFlags);
        ByteBuf encodedMessage = channel.readOutbound();
        
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();
        
        // Then
        assertThat(decodedMessage).isNotNull();
        assertThat(decodedMessage.isEncrypted()).isTrue();
        assertThat(decodedMessage.isUrgent()).isTrue();
        assertThat(decodedMessage.isAckRequired()).isTrue();
        assertThat(decodedMessage.isCompressed()).isFalse();
        assertThat(decodedMessage.isReply()).isFalse();
        
        // Verify payload content
        String decodedJson = new String(decodedMessage.getPayload(), StandardCharsets.UTF_8);
        Map<String, Object> decodedPayloadMap = MessageCodec.decode(decodedJson, Map.class);
        assertThat(decodedPayloadMap.get("message")).isEqualTo("Message with multiple flags");
    }
}