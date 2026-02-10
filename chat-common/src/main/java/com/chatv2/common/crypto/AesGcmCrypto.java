package com.chatv2.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.Key;
import java.util.Arrays;

/**
 * AES-GCM encryption/decryption utility.
 * Uses AES-256-GCM with 96-bit IV and 128-bit tag.
 *
 * <p>This class provides authenticated encryption using the AES-GCM mode.
 * It ensures both confidentiality and integrity of the encrypted data.</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Uses a cryptographically secure random number generator for IV generation</li>
 *   <li>Each encryption operation uses a unique IV (never reuse IV with the same key)</li>
 *   <li>Authenticates ciphertext using 128-bit GCM tag</li>
 *   <li>Validates all inputs before processing</li>
 * </ul>
 */
public final class AesGcmCrypto {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_SIZE = 256; // AES-256
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesGcmCrypto() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     *
     * <p>This method generates a random IV for each encryption operation, which is essential
     * for security. The IV is returned along with the ciphertext and authentication tag.</p>
     *
     * @param plaintext the plaintext to encrypt (must not be null or empty)
     * @param key the secret key (must be 32 bytes for AES-256)
     * @return EncryptionResult containing ciphertext, IV, and auth tag
     * @throws IllegalArgumentException if plaintext or key is invalid
     * @throws RuntimeException if encryption fails due to cryptographic errors
     */
    public static EncryptionResult encrypt(byte[] plaintext, Key key) {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        try {
            // Generate random IV for each encryption
            // IV must never be reused with the same key in GCM mode
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // Encrypt - doFinal returns ciphertext + auth tag
            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            // Split ciphertext and authentication tag
            // In GCM mode, the tag is appended to the ciphertext
            int ciphertextLength = ciphertextWithTag.length - GCM_TAG_LENGTH;
            if (ciphertextLength < 0) {
                throw new IllegalStateException("Invalid ciphertext length after encryption");
            }

            byte[] ciphertextBody = Arrays.copyOfRange(ciphertextWithTag, 0, ciphertextLength);
            byte[] tag = Arrays.copyOfRange(ciphertextWithTag, ciphertextLength, ciphertextWithTag.length);

            return new EncryptionResult(ciphertextBody, iv, tag);

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts ciphertext using AES-256-GCM.
     *
     * <p>This method verifies the authentication tag during decryption. Any modification
     * to the ciphertext will cause decryption to fail, ensuring integrity.</p>
     *
     * @param ciphertext the ciphertext to decrypt (must not be null or empty)
     * @param iv the initialization vector (must be exactly 12 bytes)
     * @param tag the authentication tag (must be exactly 16 bytes)
     * @param key the secret key
     * @return the decrypted plaintext
     * @throws IllegalArgumentException if any input parameter is invalid
     * @throws RuntimeException if decryption fails due to cryptographic errors or tag verification failure
     */
    public static byte[] decrypt(byte[] ciphertext, byte[] iv, byte[] tag, Key key) {
        // Validate input parameters
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }
        if (iv == null || iv.length != GCM_IV_LENGTH) {
            throw new IllegalArgumentException("IV must be " + GCM_IV_LENGTH + " bytes, got: " + (iv == null ? "null" : iv.length));
        }
        if (tag == null || tag.length != GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag must be " + GCM_TAG_LENGTH + " bytes, got: " + (tag == null ? "null" : tag.length));
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        try {
            // Combine ciphertext and tag for GCM decryption
            // GCM expects the tag to be appended to the ciphertext
            ByteBuffer combined = ByteBuffer.allocate(ciphertext.length + tag.length);
            combined.put(ciphertext);
            combined.put(tag);
            byte[] combinedBytes = combined.array();

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Decrypt - this will verify the tag automatically
            // If the tag is invalid (tampering detected), AEADBadTagException will be thrown
            byte[] plaintext = cipher.doFinal(combinedBytes);

            return plaintext;

        } catch (javax.crypto.AEADBadTagException e) {
            throw new RuntimeException("Decryption failed: Authentication tag verification failed (data may have been tampered)", e);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypts a UTF-8 string and returns the combined byte array (IV + tag + ciphertext).
     *
     * @param plaintext the string to encrypt
     * @param key the secret key
     * @return combined byte array ready for transmission or storage
     */
    public static byte[] encryptString(String plaintext, Key key) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext string cannot be null");
        }
        byte[] plaintextBytes = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        EncryptionResult result = encrypt(plaintextBytes, key);
        return result.toCombinedArray();
    }

    /**
     * Decrypts a combined byte array (IV + tag + ciphertext) to a UTF-8 string.
     *
     * @param combined the combined byte array (IV + tag + ciphertext)
     * @param key the secret key
     * @return the decrypted plaintext string
     */
    public static String decryptString(byte[] combined, Key key) {
        EncryptionResult result = EncryptionResult.fromCombinedArray(combined, GCM_IV_LENGTH, GCM_TAG_LENGTH);
        byte[] plaintextBytes = decrypt(result.ciphertext(), result.iv(), result.tag(), key);
        return new String(plaintextBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Returns the GCM IV length in bytes.
     *
     * @return IV length (12 bytes = 96 bits)
     */
    public static int getIvLength() {
        return GCM_IV_LENGTH;
    }

    /**
     * Returns the GCM tag length in bytes.
     *
     * @return tag length (16 bytes = 128 bits)
     */
    public static int getTagLength() {
        return GCM_TAG_LENGTH;
    }
}
