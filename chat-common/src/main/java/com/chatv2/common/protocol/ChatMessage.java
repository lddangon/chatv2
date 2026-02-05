package com.chatv2.common.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.CRC32;

/**
 * Binary message structure for ChatV2 protocol.
 * Header is 40 bytes total.
 */
public class ChatMessage {
    // Header constants
    public static final int HEADER_SIZE = 40;
    public static final int MAGIC_NUMBER = 0x43484154; // "CHAT" in ASCII
    public static final byte PROTOCOL_VERSION = 0x01;

    // Flags
    public static final byte FLAG_ENCRYPTED = (byte) 0x80;
    public static final byte FLAG_COMPRESSED = 0x40;
    public static final byte FLAG_URGENT = 0x20;
    public static final byte FLAG_ACK_REQUIRED = 0x10;
    public static final byte FLAG_REPLY = 0x08;

    private final ProtocolMessageType messageType;
    private final byte version;
    private final byte flags;
    private final UUID messageId;
    private final long timestamp;
    private final byte[] payload;
    private final int checksum;

    public ChatMessage(
        ProtocolMessageType messageType,
        byte flags,
        UUID messageId,
        long timestamp,
        byte[] payload
    ) {
        this.messageType = messageType;
        this.version = PROTOCOL_VERSION;
        this.flags = flags;
        this.messageId = messageId != null ? messageId : UUID.randomUUID();
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.payload = payload != null ? payload : new byte[0];
        this.checksum = calculateChecksum(this.payload);
    }

    /**
     * Creates a new unencrypted message.
     */
    public static ChatMessage createUnencrypted(ProtocolMessageType type, byte[] payload) {
        return new ChatMessage(type, (byte) 0x00, UUID.randomUUID(), System.currentTimeMillis(), payload);
    }

    /**
     * Creates a new encrypted message.
     */
    public static ChatMessage createEncrypted(ProtocolMessageType type, byte[] encryptedPayload) {
        return new ChatMessage(type, FLAG_ENCRYPTED, UUID.randomUUID(), System.currentTimeMillis(), encryptedPayload);
    }

    /**
     * Encodes message to byte array.
     */
    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payload.length);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Magic Number (4 bytes)
        buffer.putInt(MAGIC_NUMBER);

        // Message Type (2 bytes)
        buffer.putShort(messageType.getCode());

        // Version (1 byte)
        buffer.put(version);

        // Flags (1 byte)
        buffer.put(flags);

        // Message ID (8 bytes - UUID)
        putUuid(buffer, messageId);

        // Payload Length (4 bytes)
        buffer.putInt(payload.length);

        // Timestamp (8 bytes)
        buffer.putLong(timestamp);

        // Checksum (4 bytes)
        buffer.putInt(checksum);

        // Payload
        buffer.put(payload);

        return buffer.array();
    }

    /**
     * Decodes message from byte array.
     */
    public static ChatMessage decode(byte[] data) {
        if (data.length < HEADER_SIZE) {
            throw new IllegalArgumentException("Data too short to be a valid message");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data, 0, HEADER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Validate Magic Number
        int magicNumber = buffer.getInt();
        if (magicNumber != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Invalid magic number: 0x" + Integer.toHexString(magicNumber));
        }

        // Read Header Fields
        short messageTypeCode = buffer.getShort();
        ProtocolMessageType messageType = ProtocolMessageType.fromCode(messageTypeCode);
        byte version = buffer.get();
        byte flags = buffer.get();
        UUID messageId = readUuid(buffer);
        int payloadLength = buffer.getInt();
        long timestamp = buffer.getLong();
        int checksum = buffer.getInt();

        // Extract Payload
        byte[] payload;
        if (payloadLength > 0 && data.length >= HEADER_SIZE + payloadLength) {
            payload = new byte[payloadLength];
            System.arraycopy(data, HEADER_SIZE, payload, 0, payloadLength);
        } else {
            payload = new byte[0];
        }

        // Validate Checksum
        int calculatedChecksum = calculateChecksum(payload);
        if (checksum != calculatedChecksum) {
            throw new IllegalArgumentException("Checksum mismatch: expected " + checksum + ", got " + calculatedChecksum);
        }

        return new ChatMessage(messageType, flags, messageId, timestamp, payload);
    }

    /**
     * Calculates CRC32 checksum of payload.
     */
    private static int calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return (int) crc32.getValue();
    }

    /**
     * Puts UUID into buffer in network byte order.
     */
    private static void putUuid(ByteBuffer buffer, UUID uuid) {
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }

    /**
     * Reads UUID from buffer in network byte order.
     */
    private static UUID readUuid(ByteBuffer buffer) {
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    // Getters
    public ProtocolMessageType getMessageType() {
        return messageType;
    }

    public byte getVersion() {
        return version;
    }

    public byte getFlags() {
        return flags;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getChecksum() {
        return checksum;
    }

    public boolean isEncrypted() {
        return (flags & FLAG_ENCRYPTED) != 0;
    }

    public boolean isCompressed() {
        return (flags & FLAG_COMPRESSED) != 0;
    }

    public boolean isUrgent() {
        return (flags & FLAG_URGENT) != 0;
    }

    public boolean isAckRequired() {
        return (flags & FLAG_ACK_REQUIRED) != 0;
    }

    public boolean isReply() {
        return (flags & FLAG_REPLY) != 0;
    }
}
