package com.chatv2.common.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * 40-byte packet header according to PROTOCOL_SPEC.md
 *
 * Format:
 * - Magic Number (4 bytes): 0x43 0x48 0x41 0x54 ("CHAT")
 * - Message Type (2 bytes): uint16 (ProtocolMessageType code)
 * - Version (1 byte): uint8 (protocol version 0x01)
 * - Flags (1 byte): uint8 (bitfield: encrypted, compressed, urgent, etc.)
 * - Message ID (16 bytes): full UUID (mostSigBits + leastSigBits)
 * - Payload Length (4 bytes): uint32 (length of payload in bytes)
 * - Timestamp (8 bytes): uint64 (Unix milliseconds)
 * - Checksum (4 bytes): uint32 (CRC32 of payload)
 *
 * Total packet structure: [Header: 40][Payload: N]
 */
public record PacketHeader(
    int magic,              // 4 bytes: 0x43484154 ("CHAT")
    short messageType,      // 2 bytes: ProtocolMessageType code
    byte version,           // 1 byte: protocol version (0x01)
    byte flags,             // 1 byte: bitfield (ENCRYPTED, COMPRESSED, URGENT, etc.)
    long mostSigBits,       // 8 bytes: most significant bits of UUID
    long leastSigBits,      // 8 bytes: least significant bits of UUID (full UUID = 16 bytes)
    int payloadLength,      // 4 bytes: payload length in bytes
    long timestamp,         // 8 bytes: Unix milliseconds
    int checksum            // 4 bytes: CRC32 of payload
) {
    /** Size of the packet header in bytes */
    public static final int SIZE = 40;

    /** Magic number for protocol identification: "CHAT" in ASCII */
    public static final int MAGIC = 0x43484154;

    /** Current protocol version */
    public static final byte VERSION = 0x01;

    // Flag bit constants (bitfield in the flags byte)
    /** Bit 7 (0x80): Payload is encrypted with AES-256-GCM */
    public static final int FLAG_ENCRYPTED = 0x80;
    /** Bit 6 (0x40): Payload is compressed (GZIP) */
    public static final int FLAG_COMPRESSED = 0x40;
    /** Bit 5 (0x20): High priority message */
    public static final int FLAG_URGENT = 0x20;
    /** Bit 4 (0x10): Acknowledgment required */
    public static final int FLAG_ACK_REQUIRED = 0x10;
    /** Bit 3 (0x08): This is a reply to previous message */
    public static final int FLAG_REPLY = 0x08;

    /**
     * Reads a PacketHeader from the provided ByteBuffer.
     * The buffer must be in BIG_ENDIAN byte order.
     * Reads exactly 40 bytes from the buffer position.
     *
     * @param buffer ByteBuffer containing header data
     * @return PacketHeader instance
     * @throws IllegalArgumentException if buffer has insufficient data
     */
    public static PacketHeader read(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (buffer.remaining() < SIZE) {
            throw new IllegalArgumentException("Insufficient data for header: need " + SIZE + " bytes, have " + buffer.remaining());
        }

        buffer.order(ByteOrder.BIG_ENDIAN);
        int magic = buffer.getInt();
        short messageType = buffer.getShort();
        byte version = buffer.get();
        byte flags = buffer.get();
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        int payloadLength = buffer.getInt();
        long timestamp = buffer.getLong();
        int checksum = buffer.getInt();

        return new PacketHeader(magic, messageType, version, flags, mostSigBits, leastSigBits, payloadLength, timestamp, checksum);
    }

    /**
     * Writes this PacketHeader to the provided ByteBuffer.
     * The buffer must be in BIG_ENDIAN byte order.
     * Writes exactly 40 bytes to the buffer position.
     *
     * @param buffer ByteBuffer to write header data to
     * @throws IllegalArgumentException if buffer is null or has insufficient space
     */
    public void write(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        if (buffer.remaining() < SIZE) {
            throw new IllegalArgumentException("Insufficient space in buffer: need " + SIZE + " bytes, have " + buffer.remaining());
        }

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(magic);
        buffer.putShort(messageType);
        buffer.put(version);
        buffer.put(flags);
        buffer.putLong(mostSigBits);
        buffer.putLong(leastSigBits);
        buffer.putInt(payloadLength);
        buffer.putLong(timestamp);
        buffer.putInt(checksum);
    }

    /**
     * Creates a new PacketHeader with the specified fields.
     *
     * @param messageType ProtocolMessageType code
     * @param flags bitfield flags
     * @param messageId full UUID for message identification
     * @param payloadLength length of payload in bytes
     * @param payload payload data for checksum calculation
     * @return new PacketHeader instance
     */
    public static PacketHeader create(short messageType, byte flags, UUID messageId, int payloadLength, byte[] payload) {
        long mostSigBits = messageId != null ? messageId.getMostSignificantBits() : 0L;
        long leastSigBits = messageId != null ? messageId.getLeastSignificantBits() : 0L;
        int checksum = calculateChecksum(payload != null ? payload : new byte[0]);

        return new PacketHeader(
            MAGIC,
            messageType,
            VERSION,
            flags,
            mostSigBits,
            leastSigBits,
            payloadLength,
            System.currentTimeMillis(),
            checksum
        );
    }

    /**
     * Calculates CRC32 checksum of payload data.
     *
     * @param payload payload data
     * @return CRC32 checksum value
     */
    private static int calculateChecksum(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return 0;
        }
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        return (int) crc32.getValue();
    }

    /**
     * Gets the full UUID from most and least significant bits.
     *
     * @return full UUID
     */
    public UUID getMessageId() {
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Checks if the encrypted flag is set.
     *
     * @return true if payload is encrypted
     */
    public boolean isEncrypted() {
        return (flags & FLAG_ENCRYPTED) != 0;
    }

    /**
     * Checks if the compressed flag is set.
     *
     * @return true if payload is compressed
     */
    public boolean isCompressed() {
        return (flags & FLAG_COMPRESSED) != 0;
    }

    /**
     * Checks if the urgent flag is set.
     *
     * @return true if message is marked as urgent
     */
    public boolean isUrgent() {
        return (flags & FLAG_URGENT) != 0;
    }

    /**
     * Checks if the ACK required flag is set.
     *
     * @return true if acknowledgment is required
     */
    public boolean isAckRequired() {
        return (flags & FLAG_ACK_REQUIRED) != 0;
    }

    /**
     * Checks if the reply flag is set.
     *
     * @return true if this message is a reply
     */
    public boolean isReply() {
        return (flags & FLAG_REPLY) != 0;
    }

    /**
     * Validates the header fields.
     *
     * @return true if header is valid
     */
    public boolean isValid() {
        return magic == MAGIC && version == VERSION && payloadLength >= 0;
    }

    /**
     * Validates the payload checksum.
     *
     * @param payload payload data to validate
     * @return true if checksum matches payload data
     */
    public boolean validateChecksum(byte[] payload) {
        int calculatedChecksum = calculateChecksum(payload != null ? payload : new byte[0]);
        return this.checksum == calculatedChecksum;
    }
}
