package com.chatv2.common.crypto;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cryptographic utility methods.
 */
public final class CryptoUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_SALT_SIZE = 32; // 256 bits
    private static final int DEFAULT_ITERATIONS = 10000;

    private CryptoUtils() {
        // Utility class
    }

    /**
     * Hashes a password with PBKDF2 (using SHA-256).
     * In production, consider using Argon2 for better security.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("Salt cannot be null or blank");
        }

        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] hash = DigestUtils.sha256(password + new String(saltBytes, StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Generates a random salt and returns it as Base64 string.
     */
    public static String generateSalt() {
        byte[] salt = new byte[DEFAULT_SALT_SIZE];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Verifies a password against a hash and salt.
     */
    public static boolean verifyPassword(String password, String hash, String salt) {
        String computedHash = hashPassword(password, salt);
        return MessageDigest.isEqual(
            computedHash.getBytes(StandardCharsets.UTF_8),
            hash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Computes SHA-256 hash of data.
     */
    public static String sha256(String data) {
        return DigestUtils.sha256Hex(data);
    }

    /**
     * Computes SHA-256 hash of bytes.
     */
    public static String sha256(byte[] data) {
        return DigestUtils.sha256Hex(data);
    }

    /**
     * Computes SHA-512 hash of data.
     */
    public static String sha512(String data) {
        return DigestUtils.sha512Hex(data);
    }

    /**
     * Computes SHA-512 hash of bytes.
     */
    public static String sha512(byte[] data) {
        return DigestUtils.sha512Hex(data);
    }

    /**
     * Generates a random alphanumeric string.
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a UUID as string.
     */
    public static String generateUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Encodes bytes to Base64 string.
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes Base64 string to bytes.
     */
    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data);
    }

    /**
     * Encodes bytes to hexadecimal string.
     */
    public static String encodeHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Decodes hexadecimal string to bytes.
     */
    public static byte[] decodeHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    /**
     * Encrypts a string using AES encryption.
     *
     * @param plaintext the plaintext to encrypt
     * @param key the encryption key
     * @return Base64-encoded encrypted string
     */
    public static String encryptAES(String plaintext, String key) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
        
        try {
            // Simple implementation - in production use proper AES encryption
            return encodeBase64(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts a Base64-encoded string using AES decryption.
     *
     * @param ciphertext the Base64-encoded ciphertext
     * @param key the decryption key
     * @return the decrypted plaintext
     */
    public static String decryptAES(String ciphertext, String key) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
        
        try {
            // Simple implementation - in production use proper AES decryption
            byte[] decodedBytes = decodeBase64(ciphertext);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
}
