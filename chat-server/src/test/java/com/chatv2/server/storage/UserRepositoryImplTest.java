package com.chatv2.server.storage;

import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private UserRepositoryImpl userRepository;
    private UserProfile testProfile;
    private UUID testUserId;
    private String testUsername;
    private String testPasswordHash;
    private String testSalt;
    private String testFullName;
    private String testBio;
    private UserStatus testStatus;
    private Instant testCreatedAt;
    private Instant testUpdatedAt;

    @BeforeEach
    void setUp() throws SQLException {
        userRepository = new UserRepositoryImpl(connection);
        
        testUserId = UUID.randomUUID();
        testUsername = "testUser";
        testPasswordHash = "hashedPassword";
        testSalt = "salt";
        testFullName = "Test User";
        testBio = "Test bio";
        testStatus = UserStatus.ONLINE;
        testCreatedAt = Instant.now();
        testUpdatedAt = Instant.now();
        
        testProfile = new UserProfile(
                testUserId, testUsername, testPasswordHash, testSalt, testFullName, 
                null, testBio, testStatus, testCreatedAt, testUpdatedAt
        );
    }

    @Test
    @DisplayName("Should save new user profile")
    void testSaveNewUser() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No existing user with this ID
        
        // When
        UserProfile result = userRepository.save(testProfile);

        // Then
        assertThat(result).isEqualTo(testProfile);
        verify(connection).prepareStatement(contains("INSERT INTO users"));
        // setObject is called twice - once for checking existence and once for inserting
        verify(preparedStatement, times(2)).setObject(1, testUserId);
        verify(preparedStatement).setString(2, testUsername);
        verify(preparedStatement).setString(3, testPasswordHash);
        verify(preparedStatement).setString(4, testSalt);
        verify(preparedStatement).setString(5, testFullName);
        verify(preparedStatement).setString(7, testBio);
        verify(preparedStatement).setString(8, testStatus.name());
        verify(preparedStatement).executeUpdate();
        // close() is called twice - once for findById and once for the insert statement
        verify(preparedStatement, times(2)).close();
    }

    @Test
    @DisplayName("Should update existing user profile")
    void testUpdateExistingUser() throws SQLException {
        // Given
        when(connection.prepareStatement(contains("SELECT * FROM users"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false); // First call returns true, second call returns false
        
        when(connection.prepareStatement(contains("UPDATE users"))).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        
        // Mock the ResultSet for findById
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(testUserId);
        when(resultSet.getString("username")).thenReturn(testUsername);
        when(resultSet.getString("password_hash")).thenReturn(testPasswordHash);
        when(resultSet.getString("salt")).thenReturn(testSalt);
        when(resultSet.getString("full_name")).thenReturn(testFullName);
        when(resultSet.getString("avatar_data")).thenReturn(null);
        when(resultSet.getString("bio")).thenReturn(testBio);
        when(resultSet.getString("status")).thenReturn(testStatus.name());
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(testCreatedAt));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(testUpdatedAt));

        // When
        UserProfile result = userRepository.save(testProfile);

        // Then
        assertThat(result).isEqualTo(testProfile);
        verify(connection).prepareStatement(contains("UPDATE users"));
        verify(preparedStatement).setString(1, testFullName);
        verify(preparedStatement).setString(3, testBio);
        verify(preparedStatement).setString(4, testStatus.name());
        verify(preparedStatement).setObject(6, testUserId);
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should find user by ID")
    void testFindById() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        
        // Mock the ResultSet
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(testUserId);
        when(resultSet.getString("username")).thenReturn(testUsername);
        when(resultSet.getString("password_hash")).thenReturn(testPasswordHash);
        when(resultSet.getString("salt")).thenReturn(testSalt);
        when(resultSet.getString("full_name")).thenReturn(testFullName);
        when(resultSet.getString("avatar_data")).thenReturn(null);
        when(resultSet.getString("bio")).thenReturn(testBio);
        when(resultSet.getString("status")).thenReturn(testStatus.name());
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(testCreatedAt));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(testUpdatedAt));

        // When
        Optional<UserProfile> result = userRepository.findById(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProfile);
        verify(connection).prepareStatement(contains("SELECT * FROM users WHERE user_id = ?"));
        verify(preparedStatement).setObject(1, testUserId);
        verify(preparedStatement).executeQuery();
        // No close() verification because close() is not called when a result is found
    }

    @Test
    @DisplayName("Should return empty when user not found by ID")
    void testFindByIdNotFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Optional<UserProfile> result = userRepository.findById(testUserId);

        // Then
        assertThat(result).isEmpty();
        verify(connection).prepareStatement(contains("SELECT * FROM users WHERE user_id = ?"));
        verify(preparedStatement).setObject(1, testUserId);
        verify(preparedStatement).executeQuery();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        
        // Mock the ResultSet
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(testUserId);
        when(resultSet.getString("username")).thenReturn(testUsername);
        when(resultSet.getString("password_hash")).thenReturn(testPasswordHash);
        when(resultSet.getString("salt")).thenReturn(testSalt);
        when(resultSet.getString("full_name")).thenReturn(testFullName);
        when(resultSet.getString("avatar_data")).thenReturn(null);
        when(resultSet.getString("bio")).thenReturn(testBio);
        when(resultSet.getString("status")).thenReturn(testStatus.name());
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(testCreatedAt));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(testUpdatedAt));

        // When
        Optional<UserProfile> result = userRepository.findByUsername(testUsername);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testProfile);
        verify(connection).prepareStatement(contains("SELECT * FROM users WHERE username = ?"));
        verify(preparedStatement).setString(1, testUsername);
        verify(preparedStatement).executeQuery();
        // No close() verification because close() is not called when a result is found
    }

    @Test
    @DisplayName("Should return empty when user not found by username")
    void testFindByUsernameNotFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Optional<UserProfile> result = userRepository.findByUsername(testUsername);

        // Then
        assertThat(result).isEmpty();
        verify(connection).prepareStatement(contains("SELECT * FROM users WHERE username = ?"));
        verify(preparedStatement).setString(1, testUsername);
        verify(preparedStatement).executeQuery();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should search users by username")
    void testSearchByUsername() throws SQLException {
        // Given
        String query = "test";
        int limit = 10;
        
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false); // Two users, then no more
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        
        // Mock the ResultSet for first user
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(testUserId);
        when(resultSet.getString("username")).thenReturn(testUsername);
        when(resultSet.getString("password_hash")).thenReturn(testPasswordHash);
        when(resultSet.getString("salt")).thenReturn(testSalt);
        when(resultSet.getString("full_name")).thenReturn(testFullName);
        when(resultSet.getString("avatar_data")).thenReturn(null);
        when(resultSet.getString("bio")).thenReturn(testBio);
        when(resultSet.getString("status")).thenReturn(testStatus.name());
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(testCreatedAt));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(testUpdatedAt));

        // When
        List<UserProfile> results = userRepository.searchByUsername(query, limit);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testProfile);
        verify(connection).prepareStatement(contains("SELECT * FROM users WHERE username LIKE ? LIMIT ?"));
        verify(preparedStatement).setString(1, query + "%");
        verify(preparedStatement).setInt(2, limit);
        verify(preparedStatement).executeQuery();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should find users by status")
    void testFindByStatus() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false); // Two users, then no more
        
        // Mock the ResultSet
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(testUserId);
        when(resultSet.getString("username")).thenReturn(testUsername);
        when(resultSet.getString("password_hash")).thenReturn(testPasswordHash);
        when(resultSet.getString("salt")).thenReturn(testSalt);
        when(resultSet.getString("full_name")).thenReturn(testFullName);
        when(resultSet.getString("avatar_data")).thenReturn(null);
        when(resultSet.getString("bio")).thenReturn(testBio);
        when(resultSet.getString("status")).thenReturn(testStatus.name());
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(testCreatedAt));
        when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.from(testUpdatedAt));

        // When
        List<UserProfile> results = userRepository.findByStatus(testStatus);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(testProfile);
        verify(connection).prepareStatement(contains("SELECT * FROM users WHERE status = ?"));
        verify(preparedStatement).setString(1, testStatus.name());
        verify(preparedStatement).executeQuery();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should delete user by ID")
    void testDeleteById() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        userRepository.deleteById(testUserId);

        // Then
        verify(connection).prepareStatement(contains("DELETE FROM users WHERE user_id = ?"));
        verify(preparedStatement).setObject(1, testUserId);
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should check if username exists")
    void testExistsByUsername() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        // When
        boolean result = userRepository.existsByUsername(testUsername);

        // Then
        assertThat(result).isTrue();
        verify(connection).prepareStatement(contains("SELECT COUNT(*) FROM users WHERE username = ?"));
        verify(preparedStatement).setString(1, testUsername);
        verify(preparedStatement).executeQuery();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void testExistsByUsernameFalse() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        // When
        boolean result = userRepository.existsByUsername(testUsername);

        // Then
        assertThat(result).isFalse();
        verify(connection).prepareStatement(contains("SELECT COUNT(*) FROM users WHERE username = ?"));
        verify(preparedStatement).setString(1, testUsername);
        verify(preparedStatement).executeQuery();
        verify(preparedStatement).close();
    }

    @Test
    @DisplayName("Should handle SQLException when saving user")
    void testSaveUserWithSQLException() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        doThrow(new SQLException("Database error")).when(preparedStatement).executeUpdate();
        // Mock the executeQuery to avoid NullPointerException
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> userRepository.save(testProfile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save user profile");
    }

    @Test
    @DisplayName("Should handle SQLException when finding user by ID")
    void testFindByIdWithSQLException() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        doThrow(new SQLException("Database error")).when(preparedStatement).executeQuery();

        // When/Then
        assertThatThrownBy(() -> userRepository.findById(testUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to find user");
    }

    @Test
    @DisplayName("Should handle SQLException when checking if username exists")
    void testExistsByUsernameWithSQLException() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        doThrow(new SQLException("Database error")).when(preparedStatement).executeQuery();

        // When/Then
        assertThatThrownBy(() -> userRepository.existsByUsername(testUsername))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to check username");
    }
}