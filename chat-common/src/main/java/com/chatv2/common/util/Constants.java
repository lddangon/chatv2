package com.chatv2.common.util;

/**
 * Application constants.
 */
public final class Constants {
    private Constants() {
        // Utility class
    }

    // Application info
    public static final String APP_NAME = "ChatV2";
    public static final String APP_VERSION = "1.0.0";
    public static final int PROTOCOL_VERSION = 1;

    // Network
    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 8080;
    public static final int UDP_MULTICAST_PORT = 9999;
    public static final String UDP_MULTICAST_ADDRESS = "239.255.255.250";

    // Encryption
    public static final int AES_KEY_SIZE = 256;
    public static final int RSA_KEY_SIZE = 4096;
    public static final int AES_IV_SIZE = 12;
    public static final int AES_TAG_SIZE = 16;

    // Session
    public static final int SESSION_EXPIRATION_SECONDS = 3600; // 1 hour
    public static final int SESSION_REFRESH_DAYS = 7;

    // Messages
    public static final int MAX_MESSAGE_SIZE = 10_485_760; // 10MB
    public static final int MAX_MESSAGE_LENGTH = 10000; // characters

    // Users
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;
    public static final int MAX_BIO_LENGTH = 500;
    public static final int MAX_FULL_NAME_LENGTH = 100;

    // Chats
    public static final int MAX_CHAT_NAME_LENGTH = 100;
    public static final int MAX_CHAT_DESCRIPTION_LENGTH = 500;
    public static final int MAX_CHAT_PARTICIPANTS = 1000;

    // Files
    public static final String DATA_DIR = "data";
    public static final String DATABASE_FILE = "chat.db";
    public static final String LOGS_DIR = "logs";
    public static final String CONFIG_DIR = "config";
    public static final String AVATARS_DIR = "avatars";

    // Timeouts
    public static final int CONNECTION_TIMEOUT_SECONDS = 30;
    public static final int READ_TIMEOUT_SECONDS = 60;
    public static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    public static final int SERVER_DISCOVERY_TIMEOUT_SECONDS = 30;

    // Buffer sizes
    public static final int BUFFER_SIZE = 8192;
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024; // 16MB
}
