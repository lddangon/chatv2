package com.chatv2.encryption.api;

/**
 * Record representing an encryption algorithm specification.
 */
public record EncryptionAlgorithm(
    String name,
    String transformation,
    int keySize,
    int ivSize,
    int tagSize,
    KeyType keyType
) {
    public enum KeyType {
        SYMMETRIC,
        ASYMMETRIC
    }

    public EncryptionAlgorithm {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Algorithm name cannot be null or blank");
        }
        if (transformation == null || transformation.isBlank()) {
            throw new IllegalArgumentException("Algorithm transformation cannot be null or blank");
        }
        if (keySize <= 0) {
            throw new IllegalArgumentException("Key size must be positive");
        }
        if (ivSize < 0) {
            throw new IllegalArgumentException("IV size cannot be negative");
        }
        if (tagSize < 0) {
            throw new IllegalArgumentException("Tag size cannot be negative");
        }
        if (keyType == null) {
            throw new IllegalArgumentException("Key type cannot be null");
        }
    }

    /**
     * Creates AES-256-GCM algorithm specification.
     */
    public static EncryptionAlgorithm aes256Gcm() {
        return new EncryptionAlgorithm(
            "AES-256-GCM",
            "AES/GCM/NoPadding",
            256,
            12,
            16,
            KeyType.SYMMETRIC
        );
    }

    /**
     * Creates RSA-4096 algorithm specification.
     */
    public static EncryptionAlgorithm rsa4096() {
        return new EncryptionAlgorithm(
            "RSA-4096",
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding",
            4096,
            0,
            0,
            KeyType.ASYMMETRIC
        );
    }
}
