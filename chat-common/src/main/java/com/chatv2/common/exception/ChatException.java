package com.chatv2.common.exception;

/**
 * Base exception class for ChatV2 application.
 */
public class ChatException extends RuntimeException {
    private final int errorCode;

    public ChatException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ChatException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    // Error codes
    public static final int SUCCESS = 1000;
    public static final int INVALID_REQUEST = 1001;
    public static final int UNAUTHORIZED = 1002;
    public static final int FORBIDDEN = 1003;
    public static final int USER_NOT_FOUND = 1004;
    public static final int INVALID_CREDENTIALS = 1005;
    public static final int USER_EXISTS = 1006;
    public static final int SESSION_EXPIRED = 1007;
    public static final int CHAT_NOT_FOUND = 1008;
    public static final int MESSAGE_NOT_FOUND = 1009;
    public static final int ENCRYPTION_ERROR = 1010;
    public static final int NETWORK_ERROR = 1011;
    public static final int INTERNAL_ERROR = 1012;
    public static final int RATE_LIMIT_EXCEEDED = 1013;
    public static final int INVALID_TOKEN = 1014;
    public static final int INSUFFICIENT_PERMISSION = 1015;
    public static final int QUOTA_EXCEEDED = 1016;
    public static final int MALFORMED_PAYLOAD = 1017;
    public static final int CHECKSUM_MISMATCH = 1018;
    public static final int UNSUPPORTED_VERSION = 1019;
    public static final int INVALID_ENCRYPTION = 1020;
}
