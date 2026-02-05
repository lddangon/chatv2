package com.chatv2.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Session record representing a user session.
 * Immutable data class following Java 21 record pattern.
 */
public record Session(
    UUID sessionId,
    UUID userId,
    String token,
    Instant expiresAt,
    Instant createdAt,
    Instant lastAccessedAt,
    String deviceInfo
) {
    /**
     * Creates a new Session with current timestamp.
     */
    public Session {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token cannot be null or blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt cannot be null");
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastAccessedAt == null) {
            lastAccessedAt = now;
        }
    }

    /**
     * Creates a new session.
     */
    public static Session createNew(UUID userId, String token, Instant expiresAt, String deviceInfo) {
        return new Session(
            UUID.randomUUID(),
            userId,
            token,
            expiresAt,
            Instant.now(),
            Instant.now(),
            deviceInfo
        );
    }

    /**
     * Checks if session is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if session is valid (not expired).
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Updates last accessed time.
     */
    public Session withLastAccessed() {
        return new Session(
            this.sessionId,
            this.userId,
            this.token,
            this.expiresAt,
            this.createdAt,
            Instant.now(),
            this.deviceInfo
        );
    }
}
