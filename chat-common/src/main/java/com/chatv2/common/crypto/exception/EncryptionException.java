package com.chatv2.common.crypto.exception;

/**
 * Exception thrown when encryption fails.
 */
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
