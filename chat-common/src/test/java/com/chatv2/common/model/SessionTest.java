package com.chatv2.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "sessionToken123";
    private static final String DEVICE_INFO = "Windows 10 / Chrome 120.0";
    private static final Instant CREATED_AT = Instant.now();
    private static final Instant LAST_ACCESSED_AT = Instant.now();
    private static final Instant EXPIRES_AT = Instant.now().plusSeconds(3600); // 1 hour from now

    @Test
    @DisplayName("Should create Session with all fields")
    void testSessionCreation() {
        // When
        Session session = new Session(
                SESSION_ID, USER_ID, TOKEN, EXPIRES_AT, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        );

        // Then
        assertThat(session.sessionId()).isEqualTo(SESSION_ID);
        assertThat(session.userId()).isEqualTo(USER_ID);
        assertThat(session.token()).isEqualTo(TOKEN);
        assertThat(session.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(session.createdAt()).isEqualTo(CREATED_AT);
        assertThat(session.lastAccessedAt()).isEqualTo(LAST_ACCESSED_AT);
        assertThat(session.deviceInfo()).isEqualTo(DEVICE_INFO);
    }

    @Test
    @DisplayName("Should create Session with current time when createdAt is null")
    void testSessionCreationWithNullCreatedAt() {
        // When
        Instant beforeCreation = Instant.now();
        Session session = new Session(
                SESSION_ID, USER_ID, TOKEN, EXPIRES_AT, null, LAST_ACCESSED_AT, DEVICE_INFO
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(session.createdAt()).isBetween(beforeCreation, afterCreation);
    }

    @Test
    @DisplayName("Should create Session with current time when lastAccessedAt is null")
    void testSessionCreationWithNullLastAccessedAt() {
        // When
        Instant beforeCreation = Instant.now();
        Session session = new Session(
                SESSION_ID, USER_ID, TOKEN, EXPIRES_AT, CREATED_AT, null, DEVICE_INFO
        );
        Instant afterCreation = Instant.now();

        // Then
        assertThat(session.lastAccessedAt()).isBetween(beforeCreation, afterCreation);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when sessionId is null")
    void testSessionCreationWithNullSessionId(UUID sessionId) {
        // When/Then
        assertThatThrownBy(() -> new Session(
                sessionId, USER_ID, TOKEN, EXPIRES_AT, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sessionId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when userId is null")
    void testSessionCreationWithNullUserId(UUID userId) {
        // When/Then
        assertThatThrownBy(() -> new Session(
                SESSION_ID, userId, TOKEN, EXPIRES_AT, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId cannot be null");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when token is null or blank")
    void testSessionCreationWithInvalidToken(String token) {
        // When/Then
        assertThatThrownBy(() -> new Session(
                SESSION_ID, USER_ID, token, EXPIRES_AT, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token cannot be null or blank");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when expiresAt is null")
    void testSessionCreationWithNullExpiresAt(Instant expiresAt) {
        // When/Then
        assertThatThrownBy(() -> new Session(
                SESSION_ID, USER_ID, TOKEN, expiresAt, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresAt cannot be null");
    }

    @Test
    @DisplayName("Should create new session with current timestamps")
    void testCreateNew() {
        // Given
        UUID userId = UUID.randomUUID();
        String token = "testToken123";
        Instant expiresAt = Instant.now().plusSeconds(7200); // 2 hours from now
        String deviceInfo = "Device info";

        // When
        Session session = Session.createNew(userId, token, expiresAt, deviceInfo);

        // Then
        assertThat(session.sessionId()).isNotNull();
        assertThat(session.userId()).isEqualTo(userId);
        assertThat(session.token()).isEqualTo(token);
        assertThat(session.expiresAt()).isEqualTo(expiresAt);
        assertThat(session.createdAt()).isNotNull();
        assertThat(session.lastAccessedAt()).isNotNull();
        assertThat(session.deviceInfo()).isEqualTo(deviceInfo);
        assertThat(session.createdAt()).isEqualTo(session.lastAccessedAt());
    }

    @Test
    @DisplayName("Should update last accessed time")
    void testWithLastAccessed() {
        // Given
        Session originalSession = new Session(
                SESSION_ID, USER_ID, TOKEN, EXPIRES_AT, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        );

        // When
        Instant beforeUpdate = Instant.now();
        Session updatedSession = originalSession.withLastAccessed();
        Instant afterUpdate = Instant.now();

        // Then
        assertThat(updatedSession.sessionId()).isEqualTo(originalSession.sessionId());
        assertThat(updatedSession.userId()).isEqualTo(originalSession.userId());
        assertThat(updatedSession.token()).isEqualTo(originalSession.token());
        assertThat(updatedSession.expiresAt()).isEqualTo(originalSession.expiresAt());
        assertThat(updatedSession.createdAt()).isEqualTo(originalSession.createdAt());
        assertThat(updatedSession.lastAccessedAt()).isBetween(beforeUpdate, afterUpdate);
        assertThat(updatedSession.lastAccessedAt()).isAfter(originalSession.lastAccessedAt());
        assertThat(updatedSession.deviceInfo()).isEqualTo(originalSession.deviceInfo());
    }

    @Test
    @DisplayName("Should check if session is expired")
    void testIsExpired() {
        // Given
        Instant past = Instant.now().minusSeconds(60);
        Instant future = Instant.now().plusSeconds(3600);

        Session expiredSession = new Session(
                SESSION_ID, USER_ID, TOKEN, past, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        );

        Session validSession = new Session(
                SESSION_ID, USER_ID, TOKEN, future, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        );

        // Then
        assertThat(expiredSession.isExpired()).isTrue();
        assertThat(validSession.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should check if session is valid")
    void testIsValid() {
        // Given
        Instant past = Instant.now().minusSeconds(60);
        Instant future = Instant.now().plusSeconds(3600);

        Session expiredSession = new Session(
                SESSION_ID, USER_ID, TOKEN, past, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        );

        Session validSession = new Session(
                SESSION_ID, USER_ID, TOKEN, future, CREATED_AT, LAST_ACCESSED_AT, DEVICE_INFO
        );

        // Then
        assertThat(expiredSession.isValid()).isFalse();
        assertThat(validSession.isValid()).isTrue();
    }
}