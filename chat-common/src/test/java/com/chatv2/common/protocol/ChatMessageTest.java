package com.chatv2.common.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {

    @Test
    @DisplayName("Should create ChatMessage with provided parameters")
    void testChatMessageCreation() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = ChatMessage.FLAG_ENCRYPTED;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test payload".getBytes();

        // When
        ChatMessage chatMessage = new ChatMessage(messageType, flags, messageId, timestamp, payload);

        // Then
        assertThat(chatMessage.getMessageType()).isEqualTo(messageType);
        assertThat(chatMessage.getFlags()).isEqualTo(flags);
        assertThat(chatMessage.getMessageId()).isEqualTo(messageId);
        assertThat(chatMessage.getTimestamp()).isEqualTo(timestamp);
        assertThat(chatMessage.getPayload()).isEqualTo(payload);
        assertThat(chatMessage.getVersion()).isEqualTo(ChatMessage.PROTOCOL_VERSION);
    }

    @Test
    @DisplayName("Should generate random messageId when null is provided")
    void testChatMessageCreationWithNullMessageId() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = 0;
        UUID nullMessageId = null;
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test payload".getBytes();

        // When
        ChatMessage chatMessage = new ChatMessage(messageType, flags, nullMessageId, timestamp, payload);

        // Then
        assertThat(chatMessage.getMessageId()).isNotNull();
    }

    @Test
    @DisplayName("Should generate current timestamp when 0 is provided")
    void testChatMessageCreationWithZeroTimestamp() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = 0;
        UUID messageId = UUID.randomUUID();
        long zeroTimestamp = 0;
        byte[] payload = "test payload".getBytes();

        // When
        long beforeCreation = System.currentTimeMillis();
        ChatMessage chatMessage = new ChatMessage(messageType, flags, messageId, zeroTimestamp, payload);
        long afterCreation = System.currentTimeMillis();

        // Then
        assertThat(chatMessage.getTimestamp()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should use empty payload when null is provided")
    void testChatMessageCreationWithNullPayload() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = 0;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] nullPayload = null;

        // When
        ChatMessage chatMessage = new ChatMessage(messageType, flags, messageId, timestamp, nullPayload);

        // Then
        assertThat(chatMessage.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("Should create unencrypted message")
    void testCreateUnencrypted() {
        // Given
        ProtocolMessageType type = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte[] payload = "test payload".getBytes();

        // When
        ChatMessage chatMessage = ChatMessage.createUnencrypted(type, payload);

        // Then
        assertThat(chatMessage.getMessageType()).isEqualTo(type);
        assertThat(chatMessage.getFlags()).isEqualTo((byte) 0x00);
        assertThat(chatMessage.getPayload()).isEqualTo(payload);
        assertThat(chatMessage.isEncrypted()).isFalse();
    }

    @Test
    @DisplayName("Should create encrypted message")
    void testCreateEncrypted() {
        // Given
        ProtocolMessageType type = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte[] payload = "encrypted payload".getBytes();

        // When
        ChatMessage chatMessage = ChatMessage.createEncrypted(type, payload);

        // Then
        assertThat(chatMessage.getMessageType()).isEqualTo(type);
        assertThat(chatMessage.getFlags()).isEqualTo(ChatMessage.FLAG_ENCRYPTED);
        assertThat(chatMessage.getPayload()).isEqualTo(payload);
        assertThat(chatMessage.isEncrypted()).isTrue();
    }

    @Test
    @DisplayName("Should encode message to byte array")
    void testEncode() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = ChatMessage.FLAG_ENCRYPTED;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test payload".getBytes();

        ChatMessage chatMessage = new ChatMessage(messageType, flags, messageId, timestamp, payload);

        // When
        byte[] encoded = chatMessage.encode();

        // Then
        assertThat(encoded).isNotNull();
        assertThat(encoded.length).isEqualTo(ChatMessage.HEADER_SIZE + payload.length);

        // Verify magic number
        ByteBuffer buffer = ByteBuffer.wrap(encoded, 0, ChatMessage.HEADER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int magicNumber = buffer.getInt();
        assertThat(magicNumber).isEqualTo(ChatMessage.MAGIC_NUMBER);

        // Verify message type
        short messageTypeCode = buffer.getShort();
        assertThat(messageTypeCode).isEqualTo(messageType.getCode());

        // Verify version
        byte version = buffer.get();
        assertThat(version).isEqualTo(ChatMessage.PROTOCOL_VERSION);

        // Verify flags
        byte encodedFlags = buffer.get();
        assertThat(encodedFlags).isEqualTo(flags);

        // Verify message ID
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        UUID decodedMessageId = new UUID(mostSigBits, leastSigBits);
        assertThat(decodedMessageId).isEqualTo(messageId);

        // Verify payload length
        int payloadLength = buffer.getInt();
        assertThat(payloadLength).isEqualTo(payload.length);

        // Verify timestamp
        long decodedTimestamp = buffer.getLong();
        assertThat(decodedTimestamp).isEqualTo(timestamp);

        // Verify checksum
        int checksum = buffer.getInt();
        assertThat(checksum).isEqualTo(chatMessage.getChecksum());
    }

    @Test
    @DisplayName("Should decode message from byte array")
    void testDecode() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = ChatMessage.FLAG_ENCRYPTED;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test payload".getBytes();

        ChatMessage originalMessage = new ChatMessage(messageType, flags, messageId, timestamp, payload);
        byte[] encoded = originalMessage.encode();

        // When
        ChatMessage decodedMessage = ChatMessage.decode(encoded);

        // Then
        assertThat(decodedMessage.getMessageType()).isEqualTo(originalMessage.getMessageType());
        assertThat(decodedMessage.getFlags()).isEqualTo(originalMessage.getFlags());
        assertThat(decodedMessage.getMessageId()).isEqualTo(originalMessage.getMessageId());
        assertThat(decodedMessage.getTimestamp()).isEqualTo(originalMessage.getTimestamp());
        assertThat(decodedMessage.getPayload()).isEqualTo(originalMessage.getPayload());
        assertThat(decodedMessage.getChecksum()).isEqualTo(originalMessage.getChecksum());
    }

    @Test
    @DisplayName("Should throw exception when decoding data shorter than header")
    void testDecodeTooShortData() {
        // Given
        byte[] shortData = new byte[ChatMessage.HEADER_SIZE - 1];

        // When/Then
        assertThatThrownBy(() -> ChatMessage.decode(shortData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Data too short to be a valid message");
    }

    @Test
    @DisplayName("Should throw exception when decoding data with invalid magic number")
    void testDecodeInvalidMagicNumber() {
        // Given
        byte[] data = new byte[ChatMessage.HEADER_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0x12345678); // Invalid magic number

        // When/Then
        assertThatThrownBy(() -> ChatMessage.decode(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid magic number:");
    }

    @Test
    @DisplayName("Should handle message with empty payload")
    void testMessageWithEmptyPayload() {
        // Given
        ProtocolMessageType messageType = ProtocolMessageType.MESSAGE_SEND_REQ;
        byte flags = 0;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] emptyPayload = new byte[0];

        ChatMessage chatMessage = new ChatMessage(messageType, flags, messageId, timestamp, emptyPayload);

        // When
        byte[] encoded = chatMessage.encode();
        ChatMessage decodedMessage = ChatMessage.decode(encoded);

        // Then
        assertThat(decodedMessage.getPayload()).isEmpty();
    }

    @Test
    @DisplayName("Should correctly identify encrypted flag")
    void testIsEncrypted() {
        // Given
        ChatMessage encryptedMessage = ChatMessage.createEncrypted(ProtocolMessageType.MESSAGE_SEND_REQ, "test".getBytes());
        ChatMessage unencryptedMessage = ChatMessage.createUnencrypted(ProtocolMessageType.MESSAGE_SEND_REQ, "test".getBytes());

        // Then
        assertThat(encryptedMessage.isEncrypted()).isTrue();
        assertThat(unencryptedMessage.isEncrypted()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify compressed flag")
    void testIsCompressed() {
        // Given
        byte compressedFlags = ChatMessage.FLAG_COMPRESSED;
        byte uncompressedFlags = 0;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test".getBytes();

        ChatMessage compressedMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, compressedFlags, messageId, timestamp, payload);
        ChatMessage uncompressedMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, uncompressedFlags, messageId, timestamp, payload);

        // Then
        assertThat(compressedMessage.isCompressed()).isTrue();
        assertThat(uncompressedMessage.isCompressed()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify urgent flag")
    void testIsUrgent() {
        // Given
        byte urgentFlags = ChatMessage.FLAG_URGENT;
        byte nonUrgentFlags = 0;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test".getBytes();

        ChatMessage urgentMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, urgentFlags, messageId, timestamp, payload);
        ChatMessage nonUrgentMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, nonUrgentFlags, messageId, timestamp, payload);

        // Then
        assertThat(urgentMessage.isUrgent()).isTrue();
        assertThat(nonUrgentMessage.isUrgent()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify ACK required flag")
    void testIsAckRequired() {
        // Given
        byte ackFlags = ChatMessage.FLAG_ACK_REQUIRED;
        byte noAckFlags = 0;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test".getBytes();

        ChatMessage ackMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, ackFlags, messageId, timestamp, payload);
        ChatMessage noAckMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, noAckFlags, messageId, timestamp, payload);

        // Then
        assertThat(ackMessage.isAckRequired()).isTrue();
        assertThat(noAckMessage.isAckRequired()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify reply flag")
    void testIsReply() {
        // Given
        byte replyFlags = ChatMessage.FLAG_REPLY;
        byte nonReplyFlags = 0;
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test".getBytes();

        ChatMessage replyMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, replyFlags, messageId, timestamp, payload);
        ChatMessage nonReplyMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, nonReplyFlags, messageId, timestamp, payload);

        // Then
        assertThat(replyMessage.isReply()).isTrue();
        assertThat(nonReplyMessage.isReply()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify multiple flags")
    void testMultipleFlags() {
        // Given
        byte flags = (byte) (ChatMessage.FLAG_ENCRYPTED | ChatMessage.FLAG_URGENT | ChatMessage.FLAG_ACK_REQUIRED);
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test".getBytes();

        ChatMessage message = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, flags, messageId, timestamp, payload);

        // Then
        assertThat(message.isEncrypted()).isTrue();
        assertThat(message.isCompressed()).isFalse();
        assertThat(message.isUrgent()).isTrue();
        assertThat(message.isAckRequired()).isTrue();
        assertThat(message.isReply()).isFalse();
    }

    @Test
    @DisplayName("Should handle encode-decode with all flags set")
    void testEncodeDecodeWithAllFlags() {
        // Given
        byte flags = (byte) (ChatMessage.FLAG_ENCRYPTED | ChatMessage.FLAG_COMPRESSED | 
                               ChatMessage.FLAG_URGENT | ChatMessage.FLAG_ACK_REQUIRED | ChatMessage.FLAG_REPLY);
        UUID messageId = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();
        byte[] payload = "test payload with all flags".getBytes();

        ChatMessage originalMessage = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_REQ, flags, messageId, timestamp, payload);

        // When
        byte[] encoded = originalMessage.encode();
        ChatMessage decodedMessage = ChatMessage.decode(encoded);

        // Then
        assertThat(decodedMessage.isEncrypted()).isTrue();
        assertThat(decodedMessage.isCompressed()).isTrue();
        assertThat(decodedMessage.isUrgent()).isTrue();
        assertThat(decodedMessage.isAckRequired()).isTrue();
        assertThat(decodedMessage.isReply()).isTrue();
    }
}