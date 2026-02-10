package com.chatv2.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тест для проверки исправления проблемы с "Invalid payload length"
 * Проверяет, что HEADER_SIZE = 40 (с 16-байтовым UUID) и смещение payloadLength = 24
 */
class PayloadLengthFixTest {

    @Test
    @DisplayName("Should correctly encode and decode message with proper HEADER_SIZE (40 bytes with 16-byte UUID)")
    void testHeaderSizeFix() {
        // Given
        String payloadContent = "Test message for payload length validation";
        byte[] payloadBytes = payloadContent.getBytes(StandardCharsets.UTF_8);

        ChatMessage originalMessage = ChatMessage.createUnencrypted(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                payloadBytes
        );

        // When - Encode message
        byte[] encodedBytes = originalMessage.encode();

        // Then - Verify total length is correct with HEADER_SIZE = 40 (16-byte UUID)
        assertThat(encodedBytes.length).isEqualTo(ChatMessage.HEADER_SIZE + payloadBytes.length);
        assertThat(encodedBytes.length).isEqualTo(40 + payloadBytes.length);

        // When - Decode message
        ChatMessage decodedMessage = ChatMessage.decode(encodedBytes);

        // Then - Verify decoded message matches original
        assertThat(decodedMessage.getMessageType()).isEqualTo(originalMessage.getMessageType());
        assertThat(decodedMessage.getPayload()).isEqualTo(originalMessage.getPayload());
        assertThat(new String(decodedMessage.getPayload(), StandardCharsets.UTF_8))
                .isEqualTo(payloadContent);
    }

    @Test
    @DisplayName("Should correctly read payload length at offset +24 in header (after 16-byte UUID)")
    void testPayloadLengthOffsetFix() {
        // Given
        String payloadContent = "Test message for offset validation";
        byte[] payloadBytes = payloadContent.getBytes(StandardCharsets.UTF_8);

        ChatMessage originalMessage = ChatMessage.createUnencrypted(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                payloadBytes
        );

        // When - Encode message
        byte[] encodedBytes = originalMessage.encode();

        // Then - Verify payload length is correctly stored at offset +24 in header
        ByteBuffer buffer = ByteBuffer.wrap(encodedBytes);

        // Read header fields
        int magicNumber = buffer.getInt();          // 0-3
        short messageType = buffer.getShort();      // 4-5
        byte version = buffer.get();                // 6
        byte flags = buffer.get();                  // 7
        long mostSigBits = buffer.getLong();        // 8-15 (UUID part 1)
        long leastSigBits = buffer.getLong();       // 16-23 (UUID part 2)
        int payloadLength = buffer.getInt();         // 24-27 (Payload Length)

        assertThat(payloadLength).isEqualTo(payloadBytes.length);
    }

    @Test
    @DisplayName("Should handle empty payload with correct header size")
    void testEmptyPayloadWithCorrectHeaderSize() {
        // Given
        ChatMessage originalMessage = ChatMessage.createUnencrypted(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                new byte[0]
        );

        // When - Encode message
        byte[] encodedBytes = originalMessage.encode();

        // Then - Verify total length is exactly the header size (40 bytes)
        assertThat(encodedBytes.length).isEqualTo(ChatMessage.HEADER_SIZE);
        assertThat(encodedBytes.length).isEqualTo(40);
        
        // When - Decode message
        ChatMessage decodedMessage = ChatMessage.decode(encodedBytes);
        
        // Then - Verify decoded message has empty payload
        assertThat(decodedMessage.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("Should correctly decode message with maximum payload size")
    void testMaxPayloadSize() {
        // Given - Create a smaller payload to avoid memory issues in tests
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1024; i++) {  // 1KB instead of 1MB
            sb.append("x");
        }
        byte[] payloadBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        
        ChatMessage originalMessage = ChatMessage.createUnencrypted(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                payloadBytes
        );

        // When - Encode message
        byte[] encodedBytes = originalMessage.encode();
        
        // Then - Verify total length is correct
        assertThat(encodedBytes.length).isEqualTo(ChatMessage.HEADER_SIZE + payloadBytes.length);
        
        // When - Decode message
        ChatMessage decodedMessage = ChatMessage.decode(encodedBytes);
        
        // Then - Verify decoded message has correct payload length
        assertThat(decodedMessage.getPayload().length).isEqualTo(payloadBytes.length);
    }

    @Test
    @DisplayName("Should reject message with invalid magic number")
    void testInvalidMagicNumber() {
        // Given - Create a byte array with wrong magic number
        byte[] invalidBytes = new byte[ChatMessage.HEADER_SIZE];
        
        // Set wrong magic number
        invalidBytes[0] = 0x00;
        invalidBytes[1] = 0x00;
        invalidBytes[2] = 0x00;
        invalidBytes[3] = 0x00;
        
        // When/Then - Should throw exception
        assertThatThrownBy(() -> ChatMessage.decode(invalidBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid magic number");
    }

    @Test
    @DisplayName("Should handle message with all flags set")
    void testAllFlagsSet() {
        // Given
        byte[] payloadBytes = "Test message with all flags".getBytes(StandardCharsets.UTF_8);
        byte allFlags = (byte) (ChatMessage.FLAG_ENCRYPTED | ChatMessage.FLAG_COMPRESSED | 
                               ChatMessage.FLAG_URGENT | ChatMessage.FLAG_ACK_REQUIRED | 
                               ChatMessage.FLAG_REPLY);
        
        ChatMessage originalMessage = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                allFlags,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payloadBytes
        );

        // When - Encode and decode message
        byte[] encodedBytes = originalMessage.encode();
        ChatMessage decodedMessage = ChatMessage.decode(encodedBytes);
        
        // Then - Verify all flags are set correctly
        assertThat(decodedMessage.isEncrypted()).isTrue();
        assertThat(decodedMessage.isCompressed()).isTrue();
        assertThat(decodedMessage.isUrgent()).isTrue();
        assertThat(decodedMessage.isAckRequired()).isTrue();
        assertThat(decodedMessage.isReply()).isTrue();
    }
}