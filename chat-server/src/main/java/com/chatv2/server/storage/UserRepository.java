package com.chatv2.server.storage;

import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for user data access.
 */
public interface UserRepository {
    /**
     * Saves a user profile.
     */
    UserProfile save(UserProfile profile);

    /**
     * Finds a user by ID.
     */
    Optional<UserProfile> findById(UUID userId);

    /**
     * Finds a user by username.
     */
    Optional<UserProfile> findByUsername(String username);

    /**
     * Finds users by username prefix.
     */
    List<UserProfile> searchByUsername(String query, int limit);

    /**
     * Finds users by status.
     */
    List<UserProfile> findByStatus(UserStatus status);

    /**
     * Deletes a user by ID.
     */
    void deleteById(UUID userId);

    /**
     * Checks if username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Counts all users.
     */
    int countAll();

    /**
     * Finds all users.
     */
    List<UserProfile> findAll();

    /**
     * Deletes a user by ID (alias for deleteById).
     */
    default void delete(UUID userId) {
        deleteById(userId);
    }
}
