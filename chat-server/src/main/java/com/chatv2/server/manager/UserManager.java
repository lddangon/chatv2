package com.chatv2.server.manager;

import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import com.chatv2.common.util.IdGenerator;
import com.chatv2.common.util.DateUtils;
import com.chatv2.server.storage.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User manager interface and implementation.
 * Handles user registration, authentication, and profile management.
 */
public class UserManager {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);
    private final UserRepository userRepository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public UserManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user.
     */
    public CompletableFuture<UserProfile> register(String username, String password, String fullName, String bio) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Registering new user: {}", username);

                // Check if user already exists
                if (userRepository.findByUsername(username).isPresent()) {
                    throw new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.USER_EXISTS,
                        "Username already exists: " + username
                    );
                }

                // Generate salt and hash password
                String salt = com.chatv2.common.crypto.CryptoUtils.generateSalt();
                String passwordHash = com.chatv2.common.crypto.CryptoUtils.hashPassword(password, salt);

                // Create user profile
                UserProfile profile = UserProfile.createNew(
                    username,
                    passwordHash,
                    salt,
                    fullName,
                    bio
                );

                // Save to database
                UserProfile savedProfile = userRepository.save(profile);
                log.info("User registered successfully: {}", username);

                return savedProfile.toPublicProfile();
            } catch (Exception e) {
                log.error("Failed to register user: {}", username, e);
                throw e;
            }
        }, executor);
    }

    /**
     * Authenticates a user.
     */
    public CompletableFuture<UserProfile> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Authenticating user: {}", username);

                // Find user by username
                UserProfile profile = userRepository.findByUsername(username)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.USER_NOT_FOUND,
                        "User not found: " + username
                    ));

                // Verify password
                if (!com.chatv2.common.crypto.CryptoUtils.verifyPassword(password, profile.passwordHash(), profile.salt())) {
                    log.warn("Failed login attempt for user: {}", username);
                    throw new com.chatv2.common.exception.AuthenticationException("Invalid username or password");
                }

                // Update status to online
                UserProfile updatedProfile = profile.withStatus(UserStatus.ONLINE);
                userRepository.save(updatedProfile);

                log.info("User authenticated successfully: {}", username);

                return updatedProfile.toPublicProfile();
            } catch (Exception e) {
                log.error("Failed to authenticate user: {}", username, e);
                throw e;
            }
        }, executor);
    }

    /**
     * Logs out a user.
     */
    public CompletableFuture<Void> logout(UUID userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Logging out user: {}", userId);

                // Find user
                UserProfile profile = userRepository.findById(userId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.USER_NOT_FOUND,
                        "User not found: " + userId
                    ));

                // Update status to offline
                UserProfile updatedProfile = profile.withStatus(UserStatus.OFFLINE);
                userRepository.save(updatedProfile);

                log.info("User logged out: {}", userId);
            } catch (Exception e) {
                log.error("Failed to logout user: {}", userId, e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets user profile by ID.
     */
    public CompletableFuture<UserProfile> getProfile(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Getting profile for user: {}", userId);

            UserProfile profile = userRepository.findById(userId)
                .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                    com.chatv2.common.exception.ChatException.USER_NOT_FOUND,
                    "User not found: " + userId
                ));

            return profile.toPublicProfile();
        }, executor);
    }

    /**
     * Updates user profile.
     */
    public CompletableFuture<UserProfile> updateProfile(UUID userId, String fullName, String bio, String avatarData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Updating profile for user: {}", userId);

                UserProfile profile = userRepository.findById(userId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.USER_NOT_FOUND,
                        "User not found: " + userId
                    ));

                UserProfile updatedProfile = profile.withUpdates(fullName, bio, avatarData);
                UserProfile savedProfile = userRepository.save(updatedProfile);

                log.info("Profile updated for user: {}", userId);

                return savedProfile.toPublicProfile();
            } catch (Exception e) {
                log.error("Failed to update profile for user: {}", userId, e);
                throw e;
            }
        }, executor);
    }

    /**
     * Searches users by query.
     */
    public CompletableFuture<List<UserProfile>> searchUsers(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Searching users with query: {}", query);

            List<UserProfile> results = userRepository.searchByUsername(query, limit)
                .stream()
                .map(UserProfile::toPublicProfile)
                .toList();

            log.info("Found {} users matching query: {}", results.size(), query);

            return results;
        }, executor);
    }

    /**
     * Updates user status.
     */
    public CompletableFuture<Void> updateStatus(UUID userId, UserStatus status) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Updating status for user {}: {}", userId, status);

                UserProfile profile = userRepository.findById(userId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.USER_NOT_FOUND,
                        "User not found: " + userId
                    ));

                UserProfile updatedProfile = profile.withStatus(status);
                userRepository.save(updatedProfile);

                log.debug("Status updated for user: {} -> {}", userId, status);
            } catch (Exception e) {
                log.error("Failed to update status for user: {}", userId, e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets online users.
     */
    public CompletableFuture<List<UserProfile>> getOnlineUsers() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Getting online users");

            List<UserProfile> onlineUsers = userRepository.findByStatus(UserStatus.ONLINE)
                .stream()
                .map(UserProfile::toPublicProfile)
                .toList();

            log.debug("Found {} online users", onlineUsers.size());

            return onlineUsers;
        }, executor);
    }

    /**
     * Deletes a user.
     */
    public CompletableFuture<Void> deleteUser(UUID userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Deleting user: {}", userId);

                UserProfile profile = userRepository.findById(userId)
                    .orElseThrow(() -> new com.chatv2.common.exception.ChatException(
                        com.chatv2.common.exception.ChatException.USER_NOT_FOUND,
                        "User not found: " + userId
                    ));

                userRepository.deleteById(userId);

                log.info("User deleted: {}", userId);
            } catch (Exception e) {
                log.error("Failed to delete user: {}", userId, e);
                throw e;
            }
        }, executor);
    }

    /**
     * Gets the repository instance.
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }

    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        log.info("Shutting down UserManager");
        executor.shutdown();
    }
}
