package com.chatv2.server.manager;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.chatv2.common.exception.ChatException;
import com.chatv2.common.model.Session;
import com.chatv2.common.util.DateUtils;
import com.chatv2.server.config.ServerProperties;
import com.chatv2.server.storage.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Session manager with JWT token generation and validation.
 * Handles session creation, validation, and termination using JWT tokens.
 */
public class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    
    private final SessionRepository sessionRepository;
    private final ServerProperties.SessionConfig sessionConfig;
    private final Algorithm hmacAlgorithm;
    private final JWTVerifier verifier;
    
    private final java.util.Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Creates a new SessionManager with JWT signing.
     *
     * @param sessionRepository the session repository
     * @param sessionConfig the session configuration
     * @param jwtSecret the JWT secret key for signing tokens (null to generate)
     */
    public SessionManager(SessionRepository sessionRepository, ServerProperties.SessionConfig sessionConfig, String jwtSecret) {
        this.sessionRepository = sessionRepository;
        this.sessionConfig = sessionConfig;
        
        // Generate or use provided JWT secret
        byte[] secretKey = (jwtSecret != null && !jwtSecret.isBlank())
            ? jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            : generateJwtSecret();
        
        this.hmacAlgorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(hmacAlgorithm)
            .withIssuer("chatv2-server")
            .build();
        
        log.info("SessionManager initialized with JWT HMAC-SHA256");
    }

    /**
     * Creates a new SessionManager with auto-generated JWT secret.
     *
     * @param sessionRepository the session repository
     * @param sessionConfig the session configuration
     */
    public SessionManager(SessionRepository sessionRepository, ServerProperties.SessionConfig sessionConfig) {
        this(sessionRepository, sessionConfig, null);
    }

    /**
     * Generates a random JWT secret key.
     */
    private static byte[] generateJwtSecret() {
        SecureRandom random = new SecureRandom();
        byte[] secret = new byte[64]; // 512 bits for HMAC-SHA256
        random.nextBytes(secret);
        return secret;
    }

    /**
     * Creates a new session with JWT token for a user.
     *
     * @param userId the user ID
     * @param deviceInfo the device information
     * @return CompletableFuture containing the created session
     */
    public CompletableFuture<Session> createSession(UUID userId, String deviceInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Creating session for user: {}", userId);

                // Generate JWT token
                String token = generateJwtToken(userId);
                Instant expiresAt = DateUtils.addSeconds(
                    DateUtils.now(),
                    sessionConfig.getTokenExpirationSeconds()
                );

                // Create session
                Session session = Session.createNew(userId, token, expiresAt, deviceInfo);
                Session savedSession = sessionRepository.save(session);

                // Cache in memory
                activeSessions.put(token, savedSession);

                log.info("Session created for user: {} (expires at: {})", userId, expiresAt);

                return savedSession;
            } catch (Exception e) {
                log.error("Failed to create session", e);
                throw new ChatException(ChatException.INTERNAL_ERROR, "Failed to create session", e);
            }
        }, executor);
    }

    /**
     * Generates a JWT token for a user.
     *
     * @param userId the user ID
     * @return JWT token string
     */
    private String generateJwtToken(UUID userId) {
        Instant now = DateUtils.now();
        Instant expiresAt = DateUtils.addSeconds(now, sessionConfig.getTokenExpirationSeconds());

        return com.auth0.jwt.JWT.create()
            .withIssuer("chatv2-server")
            .withSubject(userId.toString())
            .withIssuedAt(java.util.Date.from(now))
            .withExpiresAt(java.util.Date.from(expiresAt))
            .withJWTId(UUID.randomUUID().toString())
            .sign(hmacAlgorithm);
    }

    /**
     * Validates a JWT token and returns the session.
     *
     * @param token the JWT token to validate
     * @return CompletableFuture containing the validated session
     * @throws ChatException if token is invalid or expired
     */
    public CompletableFuture<Session> validateSession(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Validating session token");

                if (token == null || token.isBlank()) {
                    throw new ChatException(
                        ChatException.INVALID_CREDENTIALS,
                        "Token cannot be null or blank"
                    );
                }

                // Verify JWT token signature and claims
                DecodedJWT jwt = verifyJwtToken(token);
                UUID userId = UUID.fromString(jwt.getSubject());

                // Check cache first
                Session cachedSession = activeSessions.get(token);
                if (cachedSession != null) {
                    if (cachedSession.isValid()) {
                        // Update last accessed
                        Session updatedSession = cachedSession.withLastAccessed();
                        activeSessions.put(token, updatedSession);
                        sessionRepository.save(updatedSession);
                        return updatedSession;
                    } else {
                        // Remove expired session from cache
                        activeSessions.remove(token);
                    }
                }

                // Check database
                Optional<Session> sessionOpt = sessionRepository.findByToken(token);
                if (sessionOpt.isEmpty()) {
                    throw new ChatException(
                        ChatException.SESSION_EXPIRED,
                        "Invalid session token"
                    );
                }

                Session session = sessionOpt.get();
                if (!session.isValid()) {
                    throw new ChatException(
                        ChatException.SESSION_EXPIRED,
                        "Session has expired"
                    );
                }

                // Cache in memory and update last accessed
                Session updatedSession = session.withLastAccessed();
                activeSessions.put(token, updatedSession);
                sessionRepository.save(updatedSession);

                log.debug("Session validated for user: {}", session.userId());

                return updatedSession;
            } catch (ChatException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to validate session", e);
                throw new ChatException(ChatException.SESSION_EXPIRED, "Session validation failed", e);
            }
        }, executor);
    }

    /**
     * Verifies a JWT token.
     *
     * @param token the JWT token to verify
     * @return decoded JWT
     * @throws JWTVerificationException if verification fails
     */
    private DecodedJWT verifyJwtToken(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }

    /**
     * Validates a JWT token without database lookup.
     *
     * @param token the JWT token to validate
     * @return decoded JWT with user ID
     * @throws ChatException if token is invalid or expired
     */
    public UUID validateTokenOnly(String token) {
        try {
            DecodedJWT jwt = verifier.verify(token);
            return UUID.fromString(jwt.getSubject());
        } catch (JWTVerificationException e) {
            throw new ChatException(ChatException.INVALID_CREDENTIALS, "Invalid JWT token", e);
        }
    }

    /**
     * Refreshes a session token by generating a new JWT token.
     *
     * @param token the old session token
     * @return CompletableFuture containing the refreshed session
     */
    public CompletableFuture<Session> refreshSession(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Refreshing session token");

                Session session = sessionRepository.findByToken(token)
                    .orElseThrow(() -> new ChatException(
                        ChatException.SESSION_EXPIRED,
                        "Invalid session token"
                    ));

                if (!session.isValid()) {
                    throw new ChatException(
                        ChatException.SESSION_EXPIRED,
                        "Session has expired"
                    );
                }

                // Generate new JWT token and extend expiration
                String newToken = generateJwtToken(session.userId());
                Instant newExpiresAt = DateUtils.addSeconds(
                    DateUtils.now(),
                    sessionConfig.getTokenExpirationSeconds()
                );

                Session newSession = new Session(
                    session.sessionId(),
                    session.userId(),
                    newToken,
                    newExpiresAt,
                    session.createdAt(),
                    DateUtils.now(),
                    session.deviceInfo()
                );

                Session savedSession = sessionRepository.save(newSession);

                // Update cache
                activeSessions.remove(token);
                activeSessions.put(newToken, savedSession);

                log.info("Session refreshed for user: {}", session.userId());

                return savedSession;
            } catch (ChatException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to refresh session", e);
                throw new ChatException(ChatException.INTERNAL_ERROR, "Failed to refresh session", e);
            }
        }, executor);
    }

    /**
     * Terminates a session.
     *
     * @param token the session token to terminate
     * @return CompletableFuture that completes when termination is done
     */
    public CompletableFuture<Void> terminateSession(String token) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Terminating session");

                // Remove from cache
                activeSessions.remove(token);

                // Mark as expired in database
                Optional<Session> sessionOpt = sessionRepository.findByToken(token);
                if (sessionOpt.isPresent()) {
                    Session session = sessionOpt.get();
                    Session expiredSession = new Session(
                        session.sessionId(),
                        session.userId(),
                        session.token(),
                        DateUtils.now().minusSeconds(1), // Expired
                        session.createdAt(),
                        session.lastAccessedAt(),
                        session.deviceInfo()
                    );
                    sessionRepository.save(expiredSession);
                }

                log.info("Session terminated");
            } catch (Exception e) {
                log.error("Failed to terminate session", e);
                throw new ChatException(ChatException.INTERNAL_ERROR, "Failed to terminate session", e);
            }
        }, executor);
    }

    /**
     * Terminates all sessions for a user.
     *
     * @param userId the user ID
     * @return CompletableFuture that completes when termination is done
     */
    public CompletableFuture<Void> terminateAllUserSessions(UUID userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Terminating all sessions for user: {}", userId);

                // Find all user sessions
                java.util.List<Session> sessions = sessionRepository.findByUserId(userId);

                // Terminate each session
                for (Session session : sessions) {
                    activeSessions.remove(session.token());
                    Session expiredSession = new Session(
                        session.sessionId(),
                        session.userId(),
                        session.token(),
                        DateUtils.now().minusSeconds(1),
                        session.createdAt(),
                        session.lastAccessedAt(),
                        session.deviceInfo()
                    );
                    sessionRepository.save(expiredSession);
                }

                log.info("Terminated {} sessions for user: {}", sessions.size(), userId);
            } catch (Exception e) {
                log.error("Failed to terminate user sessions", e);
                throw new ChatException(ChatException.INTERNAL_ERROR, "Failed to terminate user sessions", e);
            }
        }, executor);
    }

    /**
     * Cleans up expired sessions.
     *
     * @return CompletableFuture that completes when cleanup is done
     */
    public CompletableFuture<Void> cleanupExpiredSessions() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Cleaning up expired sessions");

                // Find expired sessions in database
                java.util.List<Session> expiredSessions = sessionRepository.findExpiredSessions(DateUtils.now());

                // Remove from cache and database
                for (Session session : expiredSessions) {
                    activeSessions.remove(session.token());
                    sessionRepository.delete(session.sessionId());
                }

                log.info("Cleaned up {} expired sessions", expiredSessions.size());
            } catch (Exception e) {
                log.error("Failed to cleanup expired sessions", e);
                throw new ChatException(ChatException.INTERNAL_ERROR, "Failed to cleanup expired sessions", e);
            }
        }, executor);
    }

    /**
     * Gets all sessions for a user.
     *
     * @param userId the user ID
     * @return CompletableFuture containing the list of sessions
     */
    public CompletableFuture<java.util.List<Session>> getUserSessions(UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Getting sessions for user: {}", userId);
            return sessionRepository.findByUserId(userId);
        }, executor);
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        log.info("Shutting down SessionManager");
        activeSessions.clear();
        executor.shutdown();
    }
}
