package com.chatv2.encryption.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionAlgorithmTest {

    @Test
    @DisplayName("Should create EncryptionAlgorithm with valid parameters")
    void testEncryptionAlgorithmCreation() {
        // Given
        String name = "Test Algorithm";
        String transformation = "Test/Transformation";
        int keySize = 256;
        int ivSize = 16;
        int tagSize = 16;
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.SYMMETRIC;

        // When
        EncryptionAlgorithm algorithm = new EncryptionAlgorithm(
                name, transformation, keySize, ivSize, tagSize, keyType
        );

        // Then
        assertThat(algorithm.name()).isEqualTo(name);
        assertThat(algorithm.transformation()).isEqualTo(transformation);
        assertThat(algorithm.keySize()).isEqualTo(keySize);
        assertThat(algorithm.ivSize()).isEqualTo(ivSize);
        assertThat(algorithm.tagSize()).isEqualTo(tagSize);
        assertThat(algorithm.keyType()).isEqualTo(keyType);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when name is null or blank")
    void testEncryptionAlgorithmCreationWithInvalidName(String name) {
        // Given
        String transformation = "Test/Transformation";
        int keySize = 256;
        int ivSize = 16;
        int tagSize = 16;
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.SYMMETRIC;

        // When/Then
        assertThatThrownBy(() -> new EncryptionAlgorithm(
                name, transformation, keySize, ivSize, tagSize, keyType
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Algorithm name cannot be null or blank");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when transformation is null or blank")
    void testEncryptionAlgorithmCreationWithInvalidTransformation(String transformation) {
        // Given
        String name = "Test Algorithm";
        int keySize = 256;
        int ivSize = 16;
        int tagSize = 16;
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.SYMMETRIC;

        // When/Then
        assertThatThrownBy(() -> new EncryptionAlgorithm(
                name, transformation, keySize, ivSize, tagSize, keyType
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Algorithm transformation cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when key size is non-positive")
    void testEncryptionAlgorithmCreationWithInvalidKeySize() {
        // Given
        String name = "Test Algorithm";
        String transformation = "Test/Transformation";
        int invalidKeySize = 0;
        int ivSize = 16;
        int tagSize = 16;
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.SYMMETRIC;

        // When/Then
        assertThatThrownBy(() -> new EncryptionAlgorithm(
                name, transformation, invalidKeySize, ivSize, tagSize, keyType
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Key size must be positive");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when IV size is negative")
    void testEncryptionAlgorithmCreationWithInvalidIvSize() {
        // Given
        String name = "Test Algorithm";
        String transformation = "Test/Transformation";
        int keySize = 256;
        int invalidIvSize = -1;
        int tagSize = 16;
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.SYMMETRIC;

        // When/Then
        assertThatThrownBy(() -> new EncryptionAlgorithm(
                name, transformation, keySize, invalidIvSize, tagSize, keyType
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("IV size cannot be negative");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when tag size is negative")
    void testEncryptionAlgorithmCreationWithInvalidTagSize() {
        // Given
        String name = "Test Algorithm";
        String transformation = "Test/Transformation";
        int keySize = 256;
        int ivSize = 16;
        int invalidTagSize = -1;
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.SYMMETRIC;

        // When/Then
        assertThatThrownBy(() -> new EncryptionAlgorithm(
                name, transformation, keySize, ivSize, invalidTagSize, keyType
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag size cannot be negative");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when key type is null")
    void testEncryptionAlgorithmCreationWithNullKeyType(EncryptionAlgorithm.KeyType keyType) {
        // Given
        String name = "Test Algorithm";
        String transformation = "Test/Transformation";
        int keySize = 256;
        int ivSize = 16;
        int tagSize = 16;

        // When/Then
        assertThatThrownBy(() -> new EncryptionAlgorithm(
                name, transformation, keySize, ivSize, tagSize, keyType
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Key type cannot be null");
    }

    @Test
    @DisplayName("Should create AES-256-GCM algorithm specification")
    void testAes256Gcm() {
        // When
        EncryptionAlgorithm algorithm = EncryptionAlgorithm.aes256Gcm();

        // Then
        assertThat(algorithm.name()).isEqualTo("AES-256-GCM");
        assertThat(algorithm.transformation()).isEqualTo("AES/GCM/NoPadding");
        assertThat(algorithm.keySize()).isEqualTo(256);
        assertThat(algorithm.ivSize()).isEqualTo(12);
        assertThat(algorithm.tagSize()).isEqualTo(16);
        assertThat(algorithm.keyType()).isEqualTo(EncryptionAlgorithm.KeyType.SYMMETRIC);
    }

    @Test
    @DisplayName("Should create RSA-4096 algorithm specification")
    void testRsa4096() {
        // When
        EncryptionAlgorithm algorithm = EncryptionAlgorithm.rsa4096();

        // Then
        assertThat(algorithm.name()).isEqualTo("RSA-4096");
        assertThat(algorithm.transformation()).isEqualTo("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        assertThat(algorithm.keySize()).isEqualTo(4096);
        assertThat(algorithm.ivSize()).isEqualTo(0);
        assertThat(algorithm.tagSize()).isEqualTo(0);
        assertThat(algorithm.keyType()).isEqualTo(EncryptionAlgorithm.KeyType.ASYMMETRIC);
    }

    @Test
    @DisplayName("Should return the same instance when calling aes256Gcm multiple times")
    void testAes256GcmConsistency() {
        // When
        EncryptionAlgorithm algorithm1 = EncryptionAlgorithm.aes256Gcm();
        EncryptionAlgorithm algorithm2 = EncryptionAlgorithm.aes256Gcm();

        // Then
        assertThat(algorithm1).isEqualTo(algorithm2);
    }

    @Test
    @DisplayName("Should return the same instance when calling rsa4096 multiple times")
    void testRsa4096Consistency() {
        // When
        EncryptionAlgorithm algorithm1 = EncryptionAlgorithm.rsa4096();
        EncryptionAlgorithm algorithm2 = EncryptionAlgorithm.rsa4096();

        // Then
        assertThat(algorithm1).isEqualTo(algorithm2);
    }

    @Test
    @DisplayName("Should create algorithm with zero IV and tag sizes for asymmetric algorithms")
    void testAlgorithmWithZeroIvAndTagSizes() {
        // Given
        String name = "RSA Test";
        String transformation = "RSA/ECB/PKCS1Padding";
        int keySize = 2048;
        int ivSize = 0;  // IV not needed for RSA
        int tagSize = 0;  // Tag not needed for RSA
        EncryptionAlgorithm.KeyType keyType = EncryptionAlgorithm.KeyType.ASYMMETRIC;

        // When
        EncryptionAlgorithm algorithm = new EncryptionAlgorithm(
                name, transformation, keySize, ivSize, tagSize, keyType
        );

        // Then
        assertThat(algorithm.ivSize()).isEqualTo(ivSize);
        assertThat(algorithm.tagSize()).isEqualTo(tagSize);
    }
}