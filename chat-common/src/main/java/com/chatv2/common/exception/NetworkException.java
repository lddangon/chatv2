package com.chatv2.common.exception;

/**
 * Exception thrown when a network operation fails.
 */
public class NetworkException extends ChatException {
    public NetworkException(String message) {
        super(NETWORK_ERROR, message);
    }

    public NetworkException(String message, Throwable cause) {
        super(NETWORK_ERROR, message, cause);
    }
}
