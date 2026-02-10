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

class PacketHeaderTest {

    @Test
    @DisplayName("Should create PacketHeader with provided parameters")
    void testPacketHeaderCreation() {
        // Given
        int magic = PacketHeader.MAGIC;
        short messageType = ProtocolMessageType.MESSAGE_SEND_REQ.getCode();
        byte version = PacketHeader.VERSION;
        byte flags = (byte) PacketHeader.FLAG_ENCRYPTED;
        UUID messageId = UUID.randomUUID();
        int payloadLength = 100;
        long timestamp = System.currentTimeMillis();
        long compactMessageId = PacketHeader.toCompactUuid(messageId);

        // When
        PacketHeader header = new PacketHeader(
                magic, messageType, version, flags, compactMessageId, payloadLength, timestamp);

        // Then
        assertThat(header.magic()).isEqualTo(magic);
        assertThat(header.messageType()).isEqualTo(messageType);
        assertThat(header.version()).isEqualTo(version);
        assertThat(header.flags()).isEqualTo(flags);
        assertThat(header.messageId()).isEqualTo(compactMessageId);
        assertThat(header.payloadLength()).isEqualTo(payloadLength);
        assertThat(header.timestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should write header to ByteBuffer")
    void testWriteHeader() {
        // Given
        int magic = PacketHeader.MAGIC;
        short messageType = ProtocolMessageType.MESSAGE_SEND_REQ.getCode();
        byte version = PacketHeader.VERSION;
        byte flags = (byte) PacketHeader.FLAG_ENCRYPTED;
        UUID messageId = UUID.randomUUID();
        int payloadLength = 100;
        long timestamp = System.currentTimeMillis();
        long compactMessageId = PacketHeader.toCompactUuid(messageId);

        PacketHeader header = new PacketHeader(
                magic, messageType, version, flags, compactMessageId, payloadLength, timestamp);

        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.SIZE);

        // When
        header.write(buffer);

        // Then
        buffer.flip();
        assertThat(buffer.getInt()).isEqualTo(magic);
        assertThat(buffer.getShort()).isEqualTo(messageType);
        assertThat(buffer.get()).isEqualTo(version);
        assertThat(buffer.get()).isEqualTo(flags);
        assertThat(buffer.getLong()).isEqualTo(compactMessageId);
        assertThat(buffer.getInt()).isEqualTo(payloadLength);
        assertThat(buffer.getLong()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should read header from ByteBuffer")
    void testReadHeader() {
        // Given
        int magic = PacketHeader.MAGIC;
        short messageType = ProtocolMessageType.MESSAGE_SEND_REQ.getCode();
        byte version = PacketHeader.VERSION;
        byte flags = (byte) PacketHeader.FLAG_ENCRYPTED;
        UUID messageId = UUID.randomUUID();
        int payloadLength = 100;
        long timestamp = System.currentTimeMillis();
        long compactMessageId = PacketHeader.toCompactUuid(messageId);

        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(magic);
        buffer.putShort(messageType);
        buffer.put(version);
        buffer.put(flags);
        buffer.putLong(compactMessageId);
        buffer.putInt(payloadLength);
        buffer.putLong(timestamp);
        buffer.flip();

        // When
        PacketHeader header = PacketHeader.read(buffer);

        // Then
        assertThat(header.magic()).isEqualTo(magic);
        assertThat(header.messageType()).isEqualTo(messageType);
        assertThat(header.version()).isEqualTo(version);
        assertThat(header.flags()).isEqualTo(flags);
        assertThat(header.messageId()).isEqualTo(compactMessageId);
        assertThat(header.payloadLength()).isEqualTo(payloadLength);
        assertThat(header.timestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should write and read header consistently")
    void testWriteReadConsistency() {
        // Given
        int magic = PacketHeader.MAGIC;
        short messageType = ProtocolMessageType.MESSAGE_SEND_REQ.getCode();
        byte version = PacketHeader.VERSION;
        byte flags = (byte) (PacketHeader.FLAG_ENCRYPTED | PacketHeader.FLAG_URGENT);
        UUID messageId = UUID.randomUUID();
        int payloadLength = 256;
        long timestamp = System.currentTimeMillis();
        long compactMessageId = PacketHeader.toCompactUuid(messageId);

        PacketHeader originalHeader = new PacketHeader(
                magic, messageType, version, flags, compactMessageId, payloadLength, timestamp);

        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.SIZE);

        // When
        originalHeader.write(buffer);
        buffer.flip();
        PacketHeader readHeader = PacketHeader.read(buffer);

        // Then
        assertThat(readHeader).isEqualTo(originalHeader);
    }

    @Test
    @DisplayName("Should create header using factory method")
    void testCreateHeader() {
        // Given
        short messageType = ProtocolMessageType.MESSAGE_SEND_REQ.getCode();
        byte flags = (byte) (PacketHeader.FLAG_ENCRYPTED | PacketHeader.FLAG_URGENT);
        UUID messageId = UUID.randomUUID();
        int payloadLength = 512;

        // When
        PacketHeader header = PacketHeader.create(messageType, flags, messageId, payloadLength);

        // Then
        assertThat(header.magic()).isEqualTo(PacketHeader.MAGIC);
        assertThat(header.messageType()).isEqualTo(messageType);
        assertThat(header.version()).isEqualTo(PacketHeader.VERSION);
        assertThat(header.flags()).isEqualTo(flags);
        assertThat(header.messageId()).isEqualTo(PacketHeader.toCompactUuid(messageId));
        assertThat(header.payloadLength()).isEqualTo(payloadLength);
        assertThat(header.timestamp()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should correctly identify encrypted flag")
    void testIsEncrypted() {
        // Given
        byte encryptedFlags = (byte) PacketHeader.FLAG_ENCRYPTED;
        byte unencryptedFlags = 0;

        PacketHeader encryptedHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, encryptedFlags, 0L, 0, 0);
        PacketHeader unencryptedHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, unencryptedFlags, 0L, 0, 0);

        // Then
        assertThat(encryptedHeader.isEncrypted()).isTrue();
        assertThat(unencryptedHeader.isEncrypted()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify compressed flag")
    void testIsCompressed() {
        // Given
        byte compressedFlags = PacketHeader.FLAG_COMPRESSED;
        byte uncompressedFlags = 0;

        PacketHeader compressedHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, compressedFlags, 0L, 0, 0);
        PacketHeader uncompressedHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, uncompressedFlags, 0L, 0, 0);

        // Then
        assertThat(compressedHeader.isCompressed()).isTrue();
        assertThat(uncompressedHeader.isCompressed()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify urgent flag")
    void testIsUrgent() {
        // Given
        byte urgentFlags = PacketHeader.FLAG_URGENT;
        byte nonUrgentFlags = 0;

        PacketHeader urgentHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, urgentFlags, 0L, 0, 0);
        PacketHeader nonUrgentHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, nonUrgentFlags, 0L, 0, 0);

        // Then
        assertThat(urgentHeader.isUrgent()).isTrue();
        assertThat(nonUrgentHeader.isUrgent()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify ACK required flag")
    void testIsAckRequired() {
        // Given
        byte ackFlags = PacketHeader.FLAG_ACK_REQUIRED;
        byte noAckFlags = 0;

        PacketHeader ackHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, ackFlags, 0L, 0, 0);
        PacketHeader noAckHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, noAckFlags, 0L, 0, 0);

        // Then
        assertThat(ackHeader.isAckRequired()).isTrue();
        assertThat(noAckHeader.isAckRequired()).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify reply flag")
    void testIsReply() {
        // Given
        byte replyFlags = PacketHeader.FLAG_REPLY;
        byte nonReplyFlags = 0;

        PacketHeader replyHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, replyFlags, 0L, 0, 0);
        PacketHeader nonReplyHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, nonReplyFlags, 0L, 0, 0);

        // Then
        assertThat(replyHeader.isReply()).isTrue();
        assertThat(nonReplyHeader.isReply()).isFalse();
    }

    @Test
    @DisplayName("Should validate header correctly")
    void testIsValid() {
        // Given
        PacketHeader validHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, (byte) 0, 0L, 100, System.currentTimeMillis());
        PacketHeader invalidMagicHeader = new PacketHeader(
                0x12345678, (short) 1, PacketHeader.VERSION, (byte) 0, 0L, 100, System.currentTimeMillis());
        PacketHeader invalidVersionHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, (byte) 0x02, (byte) 0, 0L, 100, System.currentTimeMillis());
        PacketHeader negativeLengthHeader = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, (byte) 0, 0L, -1, System.currentTimeMillis());

        // Then
        assertThat(validHeader.isValid()).isTrue();
        assertThat(invalidMagicHeader.isValid()).isFalse();
        assertThat(invalidVersionHeader.isValid()).isFalse();
        assertThat(negativeLengthHeader.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should convert UUID to compact form and back")
    void testCompactUuidConversion() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        long compactUuid = PacketHeader.toCompactUuid(uuid);
        UUID fullUuid = new UUID(0L, compactUuid); // Simulating toFullUuid method

        // Then
        assertThat(compactUuid).isEqualTo(uuid.getLeastSignificantBits());
        assertThat(fullUuid.getLeastSignificantBits()).isEqualTo(uuid.getLeastSignificantBits());
    }

    @Test
    @DisplayName("Should handle null UUID gracefully")
    void testNullUuidHandling() {
        // When
        long compactUuid = PacketHeader.toCompactUuid(null);

        // Then
        assertThat(compactUuid).isEqualTo(0L);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw exception when reading from null buffer")
    void testReadFromNullBuffer(ByteBuffer buffer) {
        // When/Then
        assertThatThrownBy(() -> PacketHeader.read(buffer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Buffer cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when buffer has insufficient data")
    void testReadFromInsufficientBuffer() {
        // Given
        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.SIZE - 1);

        // When/Then
        assertThatThrownBy(() -> PacketHeader.read(buffer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Insufficient data for header:");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw exception when writing to null buffer")
    void testWriteToNullBuffer(ByteBuffer buffer) {
        // Given
        PacketHeader header = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, (byte) 0, 0L, 100, 0L);

        // When/Then
        assertThatThrownBy(() -> header.write(buffer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Buffer cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when buffer has insufficient space")
    void testWriteToInsufficientBuffer() {
        // Given
        PacketHeader header = new PacketHeader(
                PacketHeader.MAGIC, (short) 1, PacketHeader.VERSION, (byte) 0, 0L, 100, 0L);
        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.SIZE - 1);

        // When/Then
        assertThatThrownBy(() -> header.write(buffer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Insufficient space in buffer:");
    }
}