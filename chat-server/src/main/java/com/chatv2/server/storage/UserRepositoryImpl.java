package com.chatv2.server.storage;

import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class UserRepositoryImpl implements UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepositoryImpl.class);
    private final Connection connection;

    private static final String INSERT_SQL = """
        INSERT INTO users (user_id, username, password_hash, salt, full_name, avatar_data, bio, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE users SET full_name = ?, avatar_data = ?, bio = ?, status = ?, updated_at = ?
        WHERE user_id = ?
        """;

    private static final String FIND_BY_ID_SQL = "SELECT * FROM users WHERE user_id = ?";
    private static final String FIND_BY_USERNAME_SQL = "SELECT * FROM users WHERE username = ?";
    private static final String SEARCH_SQL = """
        SELECT * FROM users WHERE username LIKE ? LIMIT ?
        """;
    private static final String FIND_BY_STATUS_SQL = "SELECT * FROM users WHERE status = ?";
    private static final String COUNT_ALL_SQL = "SELECT COUNT(*) FROM users";
    private static final String FIND_ALL_SQL = "SELECT * FROM users";
    private static final String DELETE_SQL = "DELETE FROM users WHERE user_id = ?";
    private static final String EXISTS_SQL = "SELECT COUNT(*) FROM users WHERE username = ?";

    public UserRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public UserProfile save(UserProfile profile) {
        try {
            // Check if user exists
            Optional<UserProfile> existing = findById(profile.userId());
            if (existing.isPresent()) {
                // Update existing user
                PreparedStatement stmt = connection.prepareStatement(UPDATE_SQL);
                stmt.setString(1, profile.fullName());
                stmt.setString(2, profile.avatarData());
                stmt.setString(3, profile.bio());
                stmt.setString(4, profile.status().name());
                stmt.setTimestamp(5, Timestamp.from(profile.updatedAt()));
                stmt.setObject(6, profile.userId());
                stmt.executeUpdate();
                stmt.close();
            } else {
                // Insert new user
                PreparedStatement stmt = connection.prepareStatement(INSERT_SQL);
                stmt.setObject(1, profile.userId());
                stmt.setString(2, profile.username());
                stmt.setString(3, profile.passwordHash());
                stmt.setString(4, profile.salt());
                stmt.setString(5, profile.fullName());
                stmt.setString(6, profile.avatarData());
                stmt.setString(7, profile.bio());
                stmt.setString(8, profile.status().name());
                stmt.setTimestamp(9, Timestamp.from(profile.createdAt()));
                stmt.setTimestamp(10, Timestamp.from(profile.updatedAt()));
                stmt.executeUpdate();
                stmt.close();
            }
            return profile;
        } catch (SQLException e) {
            log.error("Failed to save user profile", e);
            throw new RuntimeException("Failed to save user profile", e);
        }
    }

    @Override
    public Optional<UserProfile> findById(UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID_SQL);
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToProfile(rs));
            }
            stmt.close();
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to find user by ID: {}", userId, e);
            throw new RuntimeException("Failed to find user", e);
        }
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_USERNAME_SQL);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToProfile(rs));
            }
            stmt.close();
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to find user by username: {}", username, e);
            throw new RuntimeException("Failed to find user", e);
        }
    }

    @Override
    public List<UserProfile> searchByUsername(String query, int limit) {
        try {
            PreparedStatement stmt = connection.prepareStatement(SEARCH_SQL);
            stmt.setString(1, query + "%");
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            List<UserProfile> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToProfile(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to search users by username: {}", query, e);
            throw new RuntimeException("Failed to search users", e);
        }
    }

    @Override
    public List<UserProfile> findByStatus(UserStatus status) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_STATUS_SQL);
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            List<UserProfile> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToProfile(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find users by status: {}", status, e);
            throw new RuntimeException("Failed to find users", e);
        }
    }

    @Override
    public void deleteById(UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_SQL);
            stmt.setObject(1, userId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to delete user: {}", userId, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        try {
            PreparedStatement stmt = connection.prepareStatement(EXISTS_SQL);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                stmt.close();
                return count > 0;
            }
            stmt.close();
            return false;
        } catch (SQLException e) {
            log.error("Failed to check if username exists: {}", username, e);
            throw new RuntimeException("Failed to check username", e);
        }
    }

    @Override
    public int countAll() {
        try {
            PreparedStatement stmt = connection.prepareStatement(COUNT_ALL_SQL);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                stmt.close();
                return count;
            }
            stmt.close();
            return 0;
        } catch (SQLException e) {
            log.error("Failed to count all users", e);
            throw new RuntimeException("Failed to count users", e);
        }
    }

    @Override
    public List<UserProfile> findAll() {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_ALL_SQL);
            ResultSet rs = stmt.executeQuery();
            List<UserProfile> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToProfile(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find all users", e);
            throw new RuntimeException("Failed to find all users", e);
        }
    }

    private UserProfile mapRowToProfile(ResultSet rs) throws SQLException {
        return new UserProfile(
            rs.getObject("user_id", UUID.class),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("salt"),
            rs.getString("full_name"),
            rs.getString("avatar_data"),
            rs.getString("bio"),
            UserStatus.fromString(rs.getString("status")),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
