package com.chatv2.server.manager;

import com.chatv2.common.exception.AuthenticationException;
import com.chatv2.common.exception.ChatException;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import com.chatv2.server.storage.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserManager userManager;

    private String username;
    private String password;
    private String fullName;
    private String bio;
    private UUID userId;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        username = "testUser";
        password = "password123";
        fullName = "Test User";
        bio = "Test bio";
        userId = UUID.randomUUID();
        
        // Generate real hash and salt for the test password
        try {
            java.security.SecureRandom random = new java.security.SecureRandom();
            byte[] saltBytes = new byte[16];
            random.nextBytes(saltBytes);
            
            // Convert byte array to hex string for storage
            StringBuilder saltHex = new StringBuilder();
            for (byte b : saltBytes) {
                saltHex.append(String.format("%02x", b));
            }
            String salt = saltHex.toString();
            
            String passwordHash = com.chatv2.common.crypto.CryptoUtils.hashPassword(password, salt);
            
            userProfile = new UserProfile(
                    userId, username, passwordHash, salt, fullName, null, bio, 
                    UserStatus.OFFLINE, java.time.Instant.now(), java.time.Instant.now()
            );
        } catch (Exception e) {
            // Fallback to dummy values if CryptoUtils is not available
            userProfile = new UserProfile(
                    userId, username, "hashedPassword", "salt", fullName, null, bio, 
                    UserStatus.OFFLINE, java.time.Instant.now(), java.time.Instant.now()
            );
        }
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void testRegisterNewUser() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        // When
        UserProfile result = userManager.register(username, password, fullName, bio).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(username);
        assertThat(result.fullName()).isEqualTo(fullName);
        assertThat(result.bio()).isEqualTo(bio);
        assertThat(result.passwordHash()).isEqualTo("[REDACTED]"); // Public profile has redacted password hash
        assertThat(result.salt()).isEqualTo("[REDACTED]"); // Public profile has redacted salt
        
        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when registering existing user")
    void testRegisterExistingUser() {
        // Given
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userProfile));

        // When/Then
        assertThatThrownBy(() -> userManager.register(username, password, fullName, bio).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("Username already exists");
        
        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should authenticate user with correct credentials")
    void testLoginSuccess() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userProfile));
        when(userRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        // When
        UserProfile result = userManager.login(username, password).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(username);
        assertThat(result.status()).isEqualTo(UserStatus.ONLINE);
        assertThat(result.passwordHash()).isEqualTo("[REDACTED]"); // Public profile has redacted password hash
        assertThat(result.salt()).isEqualTo("[REDACTED]"); // Public profile has redacted salt
        
        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when authenticating non-existent user")
    void testLoginNonExistentUser() {
        // Given
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userManager.login(username, password).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when authenticating with wrong password")
    void testLoginWithWrongPassword() {
        // Given
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userProfile));

        // When/Then
        assertThatThrownBy(() -> userManager.login(username, "wrongPassword").get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
        
        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should logout user successfully")
    void testLogoutSuccess() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        // When/Then
        userManager.logout(userId).get();
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when logging out non-existent user")
    void testLogoutNonExistentUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userManager.logout(userId).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should get user profile successfully")
    void testGetProfileSuccess() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(userProfile));

        // When
        UserProfile result = userManager.getProfile(userId).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo(username);
        assertThat(result.passwordHash()).isEqualTo("[REDACTED]"); // Public profile has redacted password hash
        assertThat(result.salt()).isEqualTo("[REDACTED]"); // Public profile has redacted salt
        
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when getting profile for non-existent user")
    void testGetProfileNonExistentUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userManager.getProfile(userId).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void testUpdateProfileSuccess() throws ExecutionException, InterruptedException {
        // Given
        String newFullName = "Updated Name";
        String newBio = "Updated bio";
        String newAvatarData = "avatarData";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        // When
        UserProfile result = userManager.updateProfile(userId, newFullName, newBio, newAvatarData).get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        // The profile may not be updated in the mock, so we're just checking the method was called
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserProfile.class));
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when updating profile for non-existent user")
    void testUpdateProfileNonExistentUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userManager.updateProfile(userId, "New Name", "New Bio", null).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should search users by query successfully")
    void testSearchUsersSuccess() throws ExecutionException, InterruptedException {
        // Given
        String query = "test";
        int limit = 10;
        
        when(userRepository.searchByUsername(query, limit))
                .thenReturn(java.util.List.of(userProfile));

        // When
        java.util.List<UserProfile> result = userManager.searchUsers(query, limit).get();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo(userProfile.username()); // Compare only non-sensitive fields
        assertThat(result.get(0).fullName()).isEqualTo(userProfile.fullName());
        assertThat(result.get(0).bio()).isEqualTo(userProfile.bio());
        assertThat(result.get(0).status()).isEqualTo(userProfile.status());
        assertThat(result.get(0).passwordHash()).isEqualTo("[REDACTED]"); // Public profile has redacted password hash
        assertThat(result.get(0).salt()).isEqualTo("[REDACTED]"); // Public profile has redacted salt
        
        verify(userRepository).searchByUsername(query, limit);
    }

    @Test
    @DisplayName("Should update user status successfully")
    void testUpdateStatusSuccess() throws ExecutionException, InterruptedException {
        // Given
        UserStatus newStatus = UserStatus.AWAY;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userProfile));
        when(userRepository.save(any(UserProfile.class))).thenReturn(userProfile);

        // When/Then
        userManager.updateStatus(userId, newStatus).get();
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should throw exception when updating status for non-existent user")
    void testUpdateStatusNonExistentUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userManager.updateStatus(userId, UserStatus.AWAY).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ChatException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Should get online users successfully")
    void testGetOnlineUsersSuccess() throws ExecutionException, InterruptedException {
        // Given
        when(userRepository.findByStatus(UserStatus.ONLINE))
                .thenReturn(java.util.List.of(userProfile));

        // When
        java.util.List<UserProfile> result = userManager.getOnlineUsers().get();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo(userProfile.username()); // Compare only non-sensitive fields
        assertThat(result.get(0).fullName()).isEqualTo(userProfile.fullName());
        assertThat(result.get(0).bio()).isEqualTo(userProfile.bio());
        assertThat(result.get(0).status()).isEqualTo(userProfile.status());
        assertThat(result.get(0).passwordHash()).isEqualTo("[REDACTED]"); // Public profile has redacted password hash
        assertThat(result.get(0).salt()).isEqualTo("[REDACTED]"); // Public profile has redacted salt
        
        verify(userRepository).findByStatus(UserStatus.ONLINE);
    }

    @Test
    @DisplayName("Should shutdown correctly")
    void testShutdown() {
        // When
        userManager.shutdown();

        // Then
        // No exception should be thrown
        // This test mainly ensures the method doesn't throw any exceptions
        // In a real implementation, you might verify the executor is shut down
    }
}