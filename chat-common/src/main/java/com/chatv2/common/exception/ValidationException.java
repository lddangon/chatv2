package com.chatv2.common.exception;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends ChatException {
    public ValidationException(String message) {
        super(INVALID_REQUEST, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(INVALID_REQUEST, message, cause);
    }
}
