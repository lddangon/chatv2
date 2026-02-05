package com.chatv2.server.storage;

import com.chatv2.common.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class SessionRepositoryImpl implements SessionRepository {
    private static final Logger log = LoggerFactory.getLogger(SessionRepositoryImpl.class);
    private final Connection connection;

    private static final String INSERT_SQL = """
        INSERT INTO sessions (session_id, user_id, token, expires_at, created_at, last_accessed_at, device_info)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE sessions SET last_accessed_at = ?, expires_at = ? WHERE session_id = ?
        """;

    private static final String FIND_BY_ID_SQL = "SELECT * FROM sessions WHERE session_id = ?";
    private static final String FIND_BY_TOKEN_SQL = "SELECT * FROM sessions WHERE token = ?";
    private static final String FIND_BY_USER_SQL = "SELECT * FROM sessions WHERE user_id = ?";
    private static final String FIND_EXPIRED_SQL = "SELECT * FROM sessions WHERE expires_at < ?";
    private static final String DELETE_SQL = "DELETE FROM sessions WHERE session_id = ?";
    private static final String DELETE_BY_USER_SQL = "DELETE FROM sessions WHERE user_id = ?";
    private static final String DELETE_EXPIRED_SQL = "DELETE FROM sessions WHERE expires_at < ?";

    public SessionRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Session save(Session session) {
        try {
            // Check if session exists
            Optional<Session> existing = findById(session.sessionId());
            if (existing.isPresent()) {
                // Update existing session
                PreparedStatement stmt = connection.prepareStatement(UPDATE_SQL);
                stmt.setTimestamp(1, Timestamp.from(session.lastAccessedAt()));
                stmt.setTimestamp(2, Timestamp.from(session.expiresAt()));
                stmt.setObject(3, session.sessionId());
                stmt.executeUpdate();
                stmt.close();
            } else {
                // Insert new session
                PreparedStatement stmt = connection.prepareStatement(INSERT_SQL);
                stmt.setObject(1, session.sessionId());
                stmt.setObject(2, session.userId());
                stmt.setString(3, session.token());
                stmt.setTimestamp(4, Timestamp.from(session.expiresAt()));
                stmt.setTimestamp(5, Timestamp.from(session.createdAt()));
                stmt.setTimestamp(6, Timestamp.from(session.lastAccessedAt()));
                stmt.setString(7, session.deviceInfo());
                stmt.executeUpdate();
                stmt.close();
            }
            return session;
        } catch (SQLException e) {
            log.error("Failed to save session", e);
            throw new RuntimeException("Failed to save session", e);
        }
    }

    @Override
    public Optional<Session> findById(UUID sessionId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID_SQL);
            stmt.setObject(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToSession(rs));
            }
            stmt.close();
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to find session by ID: {}", sessionId, e);
            throw new RuntimeException("Failed to find session", e);
        }
    }

    @Override
    public Optional<Session> findByToken(String token) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_TOKEN_SQL);
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToSession(rs));
            }
            stmt.close();
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to find session by token", e);
            throw new RuntimeException("Failed to find session", e);
        }
    }

    @Override
    public List<Session> findByUserId(UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_USER_SQL);
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();
            List<Session> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToSession(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find sessions for user: {}", userId, e);
            throw new RuntimeException("Failed to find sessions", e);
        }
    }

    @Override
    public List<Session> findExpiredSessions(Instant currentTime) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_EXPIRED_SQL);
            stmt.setTimestamp(1, Timestamp.from(currentTime));
            ResultSet rs = stmt.executeQuery();
            List<Session> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToSession(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find expired sessions", e);
            throw new RuntimeException("Failed to find expired sessions", e);
        }
    }

    @Override
    public void delete(UUID sessionId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_SQL);
            stmt.setObject(1, sessionId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to delete session: {}", sessionId, e);
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    @Override
    public void deleteByUserId(UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_BY_USER_SQL);
            stmt.setObject(1, userId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to delete sessions for user: {}", userId, e);
            throw new RuntimeException("Failed to delete sessions", e);
        }
    }

    @Override
    public int deleteExpiredSessions(Instant currentTime) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_EXPIRED_SQL);
            stmt.setTimestamp(1, Timestamp.from(currentTime));
            int deleted = stmt.executeUpdate();
            stmt.close();
            return deleted;
        } catch (SQLException e) {
            log.error("Failed to delete expired sessions", e);
            throw new RuntimeException("Failed to delete expired sessions", e);
        }
    }

    private Session mapRowToSession(ResultSet rs) throws SQLException {
        return new Session(
            rs.getObject("session_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getString("token"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("last_accessed_at").toInstant(),
            rs.getString("device_info")
        );
    }
}
