package com.chatv2.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USERNAME = "testUser";
    private static final String PASSWORD_HASH = "hashedPassword";
    private static final String SALT = "someSalt";
    private static final String FULL_NAME = "Test User";
    private static final String AVATAR_DATA = "avatarData";
    private static final String BIO = "Test bio";
    private static final UserStatus STATUS = UserStatus.ONLINE;
    private static final Instant CREATED_AT = Instant.now();
    private static final Instant UPDATED_AT = Instant.now();

    @Test
    @DisplayName("Should create UserProfile with all fields")
    void testUserProfileCreation() {
        // When
        UserProfile profile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        );

        // Then
        assertThat(profile.userId()).isEqualTo(USER_ID);
        assertThat(profile.username()).isEqualTo(USERNAME);
        assertThat(profile.passwordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(profile.salt()).isEqualTo(SALT);
        assertThat(profile.fullName()).isEqualTo(FULL_NAME);
        assertThat(profile.avatarData()).isEqualTo(AVATAR_DATA);
        assertThat(profile.bio()).isEqualTo(BIO);
        assertThat(profile.status()).isEqualTo(STATUS);
        assertThat(profile.createdAt()).isEqualTo(CREATED_AT);
        assertThat(profile.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    @DisplayName("Should create UserProfile with default status OFFLINE when null is provided")
    void testUserProfileCreationWithNullStatus() {
        // When
        UserProfile profile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, null, CREATED_AT, UPDATED_AT
        );

        // Then
        assertThat(profile.status()).isEqualTo(UserStatus.OFFLINE);
    }

    @Test
    @DisplayName("Should create UserProfile with current time when createdAt is null")
    void testUserProfileCreationWithNullCreatedAt() {
        // When
        Instant beforeCreation = Instant.now();
        UserProfile profile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, null, UPDATED_AT
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(profile.createdAt()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should create UserProfile with current time when updatedAt is null")
    void testUserProfileCreationWithNullUpdatedAt() {
        // When
        Instant beforeCreation = Instant.now();
        UserProfile profile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, null
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(profile.updatedAt()).isBetween(beforeCreation, afterCreation);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when userId is null")
    void testUserProfileCreationWithNullUserId(UUID userId) {
        // When/Then
        assertThatThrownBy(() -> new UserProfile(
                userId, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when username is null or blank")
    void testUserProfileCreationWithInvalidUsername(String username) {
        // When/Then
        assertThatThrownBy(() -> new UserProfile(
                USER_ID, username, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("username cannot be null or blank");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when passwordHash is null or blank")
    void testUserProfileCreationWithInvalidPasswordHash(String passwordHash) {
        // When/Then
        assertThatThrownBy(() -> new UserProfile(
                USER_ID, USERNAME, passwordHash, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash cannot be null or blank");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when salt is null or blank")
    void testUserProfileCreationWithInvalidSalt(String salt) {
        // When/Then
        assertThatThrownBy(() -> new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, salt, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("salt cannot be null or blank");
    }

    @Test
    @DisplayName("Should create new UserProfile with random UUID")
    void testCreateNewUserProfile() {
        // When
        UserProfile profile = UserProfile.createNew(USERNAME, PASSWORD_HASH, SALT, FULL_NAME, BIO);

        // Then
        assertThat(profile.userId()).isNotNull();
        assertThat(profile.username()).isEqualTo(USERNAME);
        assertThat(profile.passwordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(profile.salt()).isEqualTo(SALT);
        assertThat(profile.fullName()).isEqualTo(FULL_NAME);
        assertThat(profile.avatarData()).isNull();
        assertThat(profile.bio()).isEqualTo(BIO);
        assertThat(profile.status()).isEqualTo(UserStatus.OFFLINE);
        assertThat(profile.createdAt()).isNotNull();
        assertThat(profile.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update profile fields")
    void testWithUpdates() {
        // Given
        UserProfile originalProfile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        );

        String newFullName = "Updated Name";
        String newBio = "Updated bio";
        String newAvatarData = "newAvatarData";

        // When
        UserProfile updatedProfile = originalProfile.withUpdates(newFullName, newBio, newAvatarData);

        // Then
        assertThat(updatedProfile.userId()).isEqualTo(originalProfile.userId());
        assertThat(updatedProfile.username()).isEqualTo(originalProfile.username());
        assertThat(updatedProfile.passwordHash()).isEqualTo(originalProfile.passwordHash());
        assertThat(updatedProfile.salt()).isEqualTo(originalProfile.salt());
        assertThat(updatedProfile.fullName()).isEqualTo(newFullName);
        assertThat(updatedProfile.avatarData()).isEqualTo(newAvatarData);
        assertThat(updatedProfile.bio()).isEqualTo(newBio);
        assertThat(updatedProfile.status()).isEqualTo(originalProfile.status());
        assertThat(updatedProfile.createdAt()).isEqualTo(originalProfile.createdAt());
        assertThat(updatedProfile.updatedAt()).isAfter(originalProfile.updatedAt());
    }

    @Test
    @DisplayName("Should update only non-null fields when withUpdates is called")
    void testWithPartialUpdates() {
        // Given
        UserProfile originalProfile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        );

        String newBio = "Updated bio only";

        // When
        UserProfile updatedProfile = originalProfile.withUpdates(null, newBio, null);

        // Then
        assertThat(updatedProfile.fullName()).isEqualTo(originalProfile.fullName());
        assertThat(updatedProfile.avatarData()).isEqualTo(originalProfile.avatarData());
        assertThat(updatedProfile.bio()).isEqualTo(newBio);
    }

    @Test
    @DisplayName("Should update user status")
    void testWithStatus() {
        // Given
        UserProfile originalProfile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, UserStatus.OFFLINE, CREATED_AT, UPDATED_AT
        );

        // When
        UserProfile updatedProfile = originalProfile.withStatus(UserStatus.AWAY);

        // Then
        assertThat(updatedProfile.status()).isEqualTo(UserStatus.AWAY);
        assertThat(updatedProfile.updatedAt()).isAfter(originalProfile.updatedAt());
    }

    @Test
    @DisplayName("Should create public profile without sensitive data")
    void testToPublicProfile() {
        // Given
        UserProfile privateProfile = new UserProfile(
                USER_ID, USERNAME, PASSWORD_HASH, SALT, FULL_NAME, AVATAR_DATA, BIO, STATUS, CREATED_AT, UPDATED_AT
        );

        // When
        UserProfile publicProfile = privateProfile.toPublicProfile();

        // Then
        assertThat(publicProfile.userId()).isEqualTo(privateProfile.userId());
        assertThat(publicProfile.username()).isEqualTo(privateProfile.username());
        assertThat(publicProfile.passwordHash()).isEqualTo("[REDACTED]");
        assertThat(publicProfile.salt()).isEqualTo("[REDACTED]");
        assertThat(publicProfile.fullName()).isEqualTo(privateProfile.fullName());
        assertThat(publicProfile.avatarData()).isEqualTo(privateProfile.avatarData());
        assertThat(publicProfile.bio()).isEqualTo(privateProfile.bio());
        assertThat(publicProfile.status()).isEqualTo(privateProfile.status());
        assertThat(publicProfile.createdAt()).isEqualTo(privateProfile.createdAt());
        assertThat(publicProfile.updatedAt()).isEqualTo(privateProfile.updatedAt());
    }
}