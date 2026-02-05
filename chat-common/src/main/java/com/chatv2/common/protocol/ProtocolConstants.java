package com.chatv2.common.protocol;

/**
 * Protocol constants for the ChatV2 binary protocol.
 */
public final class ProtocolConstants {
    private ProtocolConstants() {
        // Utility class
    }

    // Protocol identification
    public static final String PROTOCOL_NAME = "CHAT";
    public static final int PROTOCOL_VERSION = 1;

    // Message constraints
    public static final int HEADER_SIZE = 32;
    public static final int MAX_PAYLOAD_SIZE = 10_485_760; // 10MB
    public static final int MAX_MESSAGE_SIZE = HEADER_SIZE + MAX_PAYLOAD_SIZE;

    // Encryption
    public static final String ENCRYPTION_AES_256_GCM = "AES/GCM/NoPadding";
    public static final String ENCRYPTION_RSA_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    // Key sizes
    public static final int AES_KEY_SIZE = 256; // bits
    public static final int AES_IV_SIZE = 12; // bytes (96 bits)
    public static final int AES_TAG_SIZE = 16; // bytes (128 bits)
    public static final int RSA_KEY_SIZE = 4096; // bits

    // UDP discovery
    public static final String UDP_MULTICAST_ADDRESS = "239.255.255.250";
    public static final int UDP_MULTICAST_PORT = 9999;
    public static final int UDP_TTL = 4;

    // TCP server
    public static final int DEFAULT_SERVER_PORT = 8080;
    public static final String DEFAULT_SERVER_HOST = "0.0.0.0";

    // Session
    public static final int SESSION_EXPIRATION_SECONDS = 3600; // 1 hour
    public static final int SESSION_REFRESH_DAYS = 7; // 7 days

    // Header fields (sizes in bytes)
    // HEADER_SIZE is already defined above
    public static final int MAGIC_NUMBER_SIZE = 4;
    public static final int MESSAGE_TYPE_SIZE = 2;
    public static final int VERSION_SIZE = 1;
    public static final int FLAGS_SIZE = 1;
    public static final int MESSAGE_ID_SIZE = 8;
    public static final int PAYLOAD_LENGTH_SIZE = 4;
    public static final int TIMESTAMP_SIZE = 8;
    public static final int CHECKSUM_SIZE = 4;

    // Header offsets
    public static final int MAGIC_NUMBER_OFFSET = 0;
    public static final int MESSAGE_TYPE_OFFSET = 4;
    public static final int VERSION_OFFSET = 6;
    public static final int FLAGS_OFFSET = 7;
    public static final int MESSAGE_ID_OFFSET = 8;
    public static final int PAYLOAD_LENGTH_OFFSET = 16;
    public static final int TIMESTAMP_OFFSET = 20;
    public static final int CHECKSUM_OFFSET = 28;
    public static final int PAYLOAD_OFFSET = 32;
}
