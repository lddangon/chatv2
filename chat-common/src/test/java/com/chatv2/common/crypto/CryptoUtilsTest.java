package com.chatv2.common.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoUtilsTest {

    @Test
    @DisplayName("Should hash password with salt")
    void testHashPassword() {
        // Given
        String password = "testPassword123";
        String salt = CryptoUtils.generateSalt();

        // When
        String hash = CryptoUtils.hashPassword(password, salt);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).isNotBlank();
        assertThat(hash).isNotEqualTo(password);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when password is null or blank")
    void testHashPasswordWithInvalidPassword(String password) {
        // Given
        String salt = CryptoUtils.generateSalt();

        // When/Then
        assertThatThrownBy(() -> CryptoUtils.hashPassword(password, salt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password cannot be null or blank");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when salt is null or blank")
    void testHashPasswordWithInvalidSalt(String salt) {
        // Given
        String password = "testPassword123";

        // When/Then
        assertThatThrownBy(() -> CryptoUtils.hashPassword(password, salt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Salt cannot be null or blank");
    }

    @Test
    @DisplayName("Should generate random salt")
    void testGenerateSalt() {
        // When
        String salt1 = CryptoUtils.generateSalt();
        String salt2 = CryptoUtils.generateSalt();

        // Then
        assertThat(salt1).isNotNull();
        assertThat(salt1).isNotBlank();
        assertThat(salt2).isNotNull();
        assertThat(salt2).isNotBlank();
        assertThat(salt1).isNotEqualTo(salt2); // Should be different each time
    }

    @Test
    @DisplayName("Should verify password correctly")
    void testVerifyPassword() {
        // Given
        String password = "testPassword123";
        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hashPassword(password, salt);

        // When
        boolean isValid = CryptoUtils.verifyPassword(password, hash, salt);
        boolean isInvalid = CryptoUtils.verifyPassword("wrongPassword", hash, salt);

        // Then
        assertThat(isValid).isTrue();
        assertThat(isInvalid).isFalse();
    }

    @Test
    @DisplayName("Should compute SHA-256 hash of string")
    void testSha256String() {
        // Given
        String data = "test data";

        // When
        String hash = CryptoUtils.sha256(data);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64); // SHA-256 produces 64 hex characters
        assertThat(hash).matches("^[a-f0-9]{64}$"); // Should contain only hex characters
    }

    @Test
    @DisplayName("Should compute SHA-256 hash of bytes")
    void testSha256Bytes() {
        // Given
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        // When
        String hash = CryptoUtils.sha256(data);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64); // SHA-256 produces 64 hex characters
        assertThat(hash).matches("^[a-f0-9]{64}$"); // Should contain only hex characters
    }

    @Test
    @DisplayName("Should compute same SHA-256 hash for same data in different formats")
    void testSha256Consistency() {
        // Given
        String dataString = "test data";
        byte[] dataBytes = dataString.getBytes(StandardCharsets.UTF_8);

        // When
        String hashFromString = CryptoUtils.sha256(dataString);
        String hashFromBytes = CryptoUtils.sha256(dataBytes);

        // Then
        assertThat(hashFromString).isEqualTo(hashFromBytes);
    }

    @Test
    @DisplayName("Should compute SHA-512 hash of string")
    void testSha512String() {
        // Given
        String data = "test data";

        // When
        String hash = CryptoUtils.sha512(data);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(128); // SHA-512 produces 128 hex characters
        assertThat(hash).matches("^[a-f0-9]{128}$"); // Should contain only hex characters
    }

    @Test
    @DisplayName("Should compute SHA-512 hash of bytes")
    void testSha512Bytes() {
        // Given
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        // When
        String hash = CryptoUtils.sha512(data);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(128); // SHA-512 produces 128 hex characters
        assertThat(hash).matches("^[a-f0-9]{128}$"); // Should contain only hex characters
    }

    @Test
    @DisplayName("Should compute same SHA-512 hash for same data in different formats")
    void testSha512Consistency() {
        // Given
        String dataString = "test data";
        byte[] dataBytes = dataString.getBytes(StandardCharsets.UTF_8);

        // When
        String hashFromString = CryptoUtils.sha512(dataString);
        String hashFromBytes = CryptoUtils.sha512(dataBytes);

        // Then
        assertThat(hashFromString).isEqualTo(hashFromBytes);
    }

    @Test
    @DisplayName("Should generate random alphanumeric string")
    void testGenerateRandomString() {
        // Given
        int length = 10;

        // When
        String randomString1 = CryptoUtils.generateRandomString(length);
        String randomString2 = CryptoUtils.generateRandomString(length);

        // Then
        assertThat(randomString1).hasSize(length);
        assertThat(randomString2).hasSize(length);
        assertThat(randomString1).isNotEqualTo(randomString2); // Should be different each time
    }

    @Test
    @DisplayName("Should generate random alphanumeric string with length 0")
    void testGenerateRandomStringZeroLength() {
        // Given
        int length = 0;

        // When
        String randomString = CryptoUtils.generateRandomString(length);

        // Then
        assertThat(randomString).isEmpty();
    }

    @Test
    @DisplayName("Should generate UUID as string")
    void testGenerateUuid() {
        // When
        String uuid1 = CryptoUtils.generateUuid();
        String uuid2 = CryptoUtils.generateUuid();

        // Then
        assertThat(uuid1).isNotNull();
        assertThat(uuid1).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
        assertThat(uuid2).isNotNull();
        assertThat(uuid2).matches("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");
        assertThat(uuid1).isNotEqualTo(uuid2); // Should be different each time
    }

    @Test
    @DisplayName("Should encode bytes to Base64 string")
    void testEncodeBase64() {
        // Given
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        // When
        String encoded = CryptoUtils.encodeBase64(data);

        // Then
        assertThat(encoded).isNotNull();
        assertThat(encoded).isNotBlank();
    }

    @Test
    @DisplayName("Should decode Base64 string to bytes")
    void testDecodeBase64() {
        // Given
        byte[] originalData = "test data".getBytes(StandardCharsets.UTF_8);
        String encoded = CryptoUtils.encodeBase64(originalData);

        // When
        byte[] decodedData = CryptoUtils.decodeBase64(encoded);

        // Then
        assertThat(decodedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should encode bytes to hexadecimal string")
    void testEncodeHex() {
        // Given
        byte[] data = {0x01, 0x23, 0x45, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};

        // When
        String hex = CryptoUtils.encodeHex(data);

        // Then
        assertThat(hex).isNotNull();
        assertThat(hex).isEqualTo("012345abcdef");
    }

    @Test
    @DisplayName("Should decode hexadecimal string to bytes")
    void testDecodeHex() {
        // Given
        String hex = "012345abcdef";

        // When
        byte[] data = CryptoUtils.decodeHex(hex);

        // Then
        assertThat(data).hasSize(6);
        assertThat(data[0]).isEqualTo((byte) 0x01);
        assertThat(data[1]).isEqualTo((byte) 0x23);
        assertThat(data[2]).isEqualTo((byte) 0x45);
        assertThat(data[3]).isEqualTo((byte) 0xAB);
        assertThat(data[4]).isEqualTo((byte) 0xCD);
        assertThat(data[5]).isEqualTo((byte) 0xEF);
    }

    @Test
    @DisplayName("Should encode and decode hex consistently")
    void testHexConsistency() {
        // Given
        byte[] originalData = "test data with special chars: ñáéíóú".getBytes(StandardCharsets.UTF_8);

        // When
        String hex = CryptoUtils.encodeHex(originalData);
        byte[] decodedData = CryptoUtils.decodeHex(hex);

        // Then
        assertThat(decodedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should compare byte arrays in constant time")
    void testConstantTimeEquals() {
        // Given
        byte[] data1 = "test data".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "test data".getBytes(StandardCharsets.UTF_8);
        byte[] data3 = "different data".getBytes(StandardCharsets.UTF_8);

        // When
        boolean equals1 = CryptoUtils.constantTimeEquals(data1, data2);
        boolean equals2 = CryptoUtils.constantTimeEquals(data1, data3);

        // Then
        assertThat(equals1).isTrue();
        assertThat(equals2).isFalse();
    }

    @Test
    @DisplayName("Should return false for byte arrays of different lengths")
    void testConstantTimeEqualsDifferentLengths() {
        // Given
        byte[] data1 = "short".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "much longer data".getBytes(StandardCharsets.UTF_8);

        // When
        boolean result = CryptoUtils.constantTimeEquals(data1, data2);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true for empty byte arrays")
    void testConstantTimeEqualsEmptyArrays() {
        // Given
        byte[] data1 = new byte[0];
        byte[] data2 = new byte[0];

        // When
        boolean result = CryptoUtils.constantTimeEquals(data1, data2);

        // Then
        assertThat(result).isTrue();
    }
}