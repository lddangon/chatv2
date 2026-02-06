package com.chatv2.common.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * 28-byte packet header according to PROTOCOL_SPEC.md
 *
 * Format:
 * - Magic Number (4 bytes): 0x43 0x48 0x41 0x54 ("CHAT")
 * - Message Type (2 bytes): uint16 (ProtocolMessageType code)
 * - Version (1 byte): uint8 (protocol version 0x01)
 * - Flags (1 byte): uint8 (bitfield: encrypted, compressed, urgent, etc.)
 * - Message ID (8 bytes): first 8 bytes of UUID (compact storage)
 * - Payload Length (4 bytes): uint32 (length of payload in bytes)
 * - Timestamp (8 bytes): uint64 (Unix milliseconds)
 *
 * Note: The full 4-byte checksum is stored AFTER the payload, not in the header.
 * Total packet structure: [Header: 28][Payload: N][Checksum: 4]
 */
public record PacketHeader(
    int magic,              // 4 bytes: 0x43484154 ("CHAT")
    short messageType,      // 2 bytes: ProtocolMessageType code
    byte version,           // 1 byte: protocol version (0x01)
    byte flags,             // 1 byte: bitfield (ENCRYPTED, COMPRESSED, URGENT, etc.)
    long messageId,         // 8 bytes: first 8 bytes of UUID (compact storage)
    int payloadLength,      // 4 bytes: payload length in bytes
    long timestamp          // 8 bytes: Unix milliseconds
) {
    /** Size of the packet header in bytes (excluding checksum) */
    public static final int SIZE = 28;

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
     * Reads exactly 28 bytes from the buffer position.
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
        long messageId = buffer.getLong();
        int payloadLength = buffer.getInt();
        long timestamp = buffer.getLong();

        return new PacketHeader(magic, messageType, version, flags, messageId, payloadLength, timestamp);
    }

    /**
     * Writes this PacketHeader to the provided ByteBuffer.
     * The buffer must be in BIG_ENDIAN byte order.
     * Writes exactly 28 bytes to the buffer position.
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
        buffer.putLong(messageId);
        buffer.putInt(payloadLength);
        buffer.putLong(timestamp);
    }

    /**
     * Creates a new PacketHeader with the specified fields.
     *
     * @param messageType ProtocolMessageType code
     * @param flags bitfield flags
     * @param messageId UUID to extract 8 bytes from
     * @param payloadLength length of payload in bytes
     * @return new PacketHeader instance
     */
    public static PacketHeader create(short messageType, byte flags, UUID messageId, int payloadLength) {
        return new PacketHeader(
            MAGIC,
            messageType,
            VERSION,
            flags,
            messageId != null ? messageId.getLeastSignificantBits() : 0L,
            payloadLength,
            System.currentTimeMillis()
        );
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
     * Converts the compact 8-byte message ID to a full UUID.
     * Uses the compact ID as the least significant bits and a fixed
     * most significant bits (0) for simplicity.
     *
     * @return full UUID
     */
    public UUID toFullUuid() {
        // For compact storage, we use only 8 bytes (LSB)
        // This can be expanded to full UUID if needed
        return new UUID(0L, messageId);
    }

    /**
     * Converts a full UUID to compact 8-byte storage.
     *
     * @param uuid full UUID
     * @return 8-byte compact representation
     */
    public static long toCompactUuid(UUID uuid) {
        return uuid != null ? uuid.getLeastSignificantBits() : 0L;
    }
}
