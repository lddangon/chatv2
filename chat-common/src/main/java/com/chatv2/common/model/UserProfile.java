package com.chatv2.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile record containing user information.
 * Immutable data class following Java 21 record pattern.
 */
public record UserProfile(
    UUID userId,
    String username,
    String passwordHash,
    String salt,
    String fullName,
    String avatarData,
    String bio,
    UserStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Creates a new UserProfile with current timestamp.
     */
    public UserProfile {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username cannot be null or blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash cannot be null or blank");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("salt cannot be null or blank");
        }
        if (status == null) {
            status = UserStatus.OFFLINE;
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Creates a UserProfile without ID (for new user creation).
     */
    public static UserProfile createNew(
        String username,
        String passwordHash,
        String salt,
        String fullName,
        String bio
    ) {
        return new UserProfile(
            UUID.randomUUID(),
            username,
            passwordHash,
            salt,
            fullName,
            null,
            bio,
            UserStatus.OFFLINE,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Updates specific fields of the user profile.
     */
    public UserProfile withUpdates(String fullName, String bio, String avatarData) {
        return new UserProfile(
            this.userId,
            this.username,
            this.passwordHash,
            this.salt,
            fullName != null ? fullName : this.fullName,
            avatarData != null ? avatarData : this.avatarData,
            bio != null ? bio : this.bio,
            this.status,
            this.createdAt,
            Instant.now()
        );
    }

    /**
     * Updates user status.
     */
    public UserProfile withStatus(UserStatus newStatus) {
        return new UserProfile(
            this.userId,
            this.username,
            this.passwordHash,
            this.salt,
            this.fullName,
            this.avatarData,
            this.bio,
            newStatus,
            this.createdAt,
            Instant.now()
        );
    }

    /**
     * Returns a safe copy without sensitive data (password hash and salt).
     * For public profiles, we use placeholder values for sensitive fields.
     */
    public UserProfile toPublicProfile() {
        return new UserProfile(
            this.userId,
            this.username,
            "[REDACTED]", // Placeholder for password hash
            "[REDACTED]", // Placeholder for salt
            this.fullName,
            this.avatarData,
            this.bio,
            this.status,
            this.createdAt,
            this.updatedAt
        );
    }

    /**
     * Returns the email derived from username (for simplicity in this implementation).
     */
    public String email() {
        return username + "@chatv2.local";
    }
}
