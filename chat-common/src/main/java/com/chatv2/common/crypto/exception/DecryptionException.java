package com.chatv2.common.crypto.exception;

/**
 * Exception thrown when decryption fails.
 */
public class DecryptionException extends RuntimeException {
    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
