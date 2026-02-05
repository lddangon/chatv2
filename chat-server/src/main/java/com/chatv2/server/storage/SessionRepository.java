package com.chatv2.server.storage;

import com.chatv2.common.model.Session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for session data access.
 */
public interface SessionRepository {
    /**
     * Saves a session.
     */
    Session save(Session session);

    /**
     * Finds a session by ID.
     */
    Optional<Session> findById(UUID sessionId);

    /**
     * Finds a session by token.
     */
    Optional<Session> findByToken(String token);

    /**
     * Finds all sessions for a user.
     */
    List<Session> findByUserId(UUID userId);

    /**
     * Finds expired sessions.
     */
    List<Session> findExpiredSessions(Instant currentTime);

    /**
     * Deletes a session by ID.
     */
    void delete(UUID sessionId);

    /**
     * Deletes sessions by user ID.
     */
    void deleteByUserId(UUID userId);

    /**
     * Deletes all expired sessions.
     */
    int deleteExpiredSessions(Instant currentTime);
}
