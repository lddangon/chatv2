package com.chatv2.common.exception;

/**
 * Exception thrown when authorization fails.
 */
public class AuthorizationException extends ChatException {
    public AuthorizationException(String message) {
        super(FORBIDDEN, message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(FORBIDDEN, message, cause);
    }
}
