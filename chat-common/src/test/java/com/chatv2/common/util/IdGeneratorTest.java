package com.chatv2.common.util;

import com.chatv2.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdGeneratorTest {

    @Test
    @DisplayName("Should generate random UUID")
    void testGenerateUuid() {
        // When
        UUID uuid1 = IdGenerator.generateUuid();
        UUID uuid2 = IdGenerator.generateUuid();

        // Then
        assertThat(uuid1).isNotNull();
        assertThat(uuid2).isNotNull();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    @DisplayName("Should generate UUID as string")
    void testGenerateUuidString() {
        // When
        String uuidString1 = IdGenerator.generateUuidString();
        String uuidString2 = IdGenerator.generateUuidString();

        // Then
        assertThat(uuidString1).isNotNull();
        assertThat(uuidString1).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
        
        assertThat(uuidString2).isNotNull();
        assertThat(uuidString2).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
        
        assertThat(uuidString1).isNotEqualTo(uuidString2);
    }

    @Test
    @DisplayName("Should generate short UUID string without dashes")
    void testGenerateShortUuidString() {
        // When
        String shortUuid1 = IdGenerator.generateShortUuidString();
        String shortUuid2 = IdGenerator.generateShortUuidString();

        // Then
        assertThat(shortUuid1).isNotNull();
        assertThat(shortUuid1).hasSize(32); // 36 chars with dashes minus 4 dashes = 32
        assertThat(shortUuid1).matches("^[a-f0-9]{32}$");
        assertThat(shortUuid1).doesNotContain("-");
        
        assertThat(shortUuid2).isNotNull();
        assertThat(shortUuid2).hasSize(32);
        assertThat(shortUuid2).matches("^[a-f0-9]{32}$");
        assertThat(shortUuid2).doesNotContain("-");
        
        assertThat(shortUuid1).isNotEqualTo(shortUuid2);
    }

    @Test
    @DisplayName("Should parse UUID from string")
    void testParseUuid() {
        // Given
        String validUuidString = "550e8400-e29b-41d4-a716-446655440000";
        UUID expectedUuid = UUID.fromString(validUuidString);

        // When
        UUID parsedUuid = IdGenerator.parseUuid(validUuidString);

        // Then
        assertThat(parsedUuid).isEqualTo(expectedUuid);
    }

    @Test
    @DisplayName("Should throw ValidationException when parsing invalid UUID string")
    void testParseInvalidUuid() {
        // Given
        String invalidUuidString = "invalid-uuid-string";

        // When/Then
        assertThatThrownBy(() -> IdGenerator.parseUuid(invalidUuidString))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid UUID format: " + invalidUuidString);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw ValidationException when parsing null or empty UUID string")
    void testParseNullOrEmptyUuid(String uuidString) {
        // When/Then
        assertThatThrownBy(() -> IdGenerator.parseUuid(uuidString))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Should validate correct UUID string")
    void testIsValidUuid() {
        // Given
        String validUuidString = "550e8400-e29b-41d4-a716-446655440000";

        // When
        boolean isValid = IdGenerator.isValidUuid(validUuidString);

        // Then
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {
            "invalid-uuid-string",
            "550e8400-e29b-41d4-a716-44665544", // Too short
            "550e8400-e29b-41d4-a716-4466554400000", // Too long
            "550e8400-e29b-41d4-a716-44665544zzzz", // Invalid characters
            "g50e8400-e29b-41d4-a716-446655440000"  // Invalid character 'g'
    })
    @DisplayName("Should return false for invalid UUID strings")
    void testIsNotValidUuid(String uuidString) {
        // When
        boolean isValid = IdGenerator.isValidUuid(uuidString);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate and parse consistently")
    void testValidateParseConsistency() {
        // Given
        String uuidString = IdGenerator.generateUuidString();

        // When
        boolean isValid = IdGenerator.isValidUuid(uuidString);
        UUID parsedUuid = IdGenerator.parseUuid(uuidString);
        String toString = parsedUuid.toString();

        // Then
        assertThat(isValid).isTrue();
        assertThat(toString).isEqualTo(uuidString);
    }

    @Test
    @DisplayName("Should generate and parse short UUID consistently")
    void testGenerateParseShortUuid() {
        // When
        String shortUuidString = IdGenerator.generateShortUuidString();
        
        // Convert short UUID (without dashes) to standard UUID format for parsing
        String standardUuidString = String.format("%s-%s-%s-%s-%s",
                shortUuidString.substring(0, 8),
                shortUuidString.substring(8, 12),
                shortUuidString.substring(12, 16),
                shortUuidString.substring(16, 20),
                shortUuidString.substring(20, 32));
        
        UUID parsedUuid = IdGenerator.parseUuid(standardUuidString);
        
        // Then
        assertThat(parsedUuid).isNotNull();
        assertThat(parsedUuid.toString()).isEqualTo(standardUuidString);
    }
}