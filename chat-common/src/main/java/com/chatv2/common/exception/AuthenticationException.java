package com.chatv2.common.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends ChatException {
    public AuthenticationException(String message) {
        super(INVALID_CREDENTIALS, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(INVALID_CREDENTIALS, message, cause);
    }
}
