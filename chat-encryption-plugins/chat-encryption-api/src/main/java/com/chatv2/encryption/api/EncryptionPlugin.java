package com.chatv2.encryption.api;

import com.chatv2.common.crypto.EncryptionResult;

import java.security.Key;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for encryption plugins.
 * Plugins are discovered via SPI (Service Provider Interface).
 */
public interface EncryptionPlugin {

    /**
     * Returns the plugin name (e.g., "AES-256", "RSA-4096").
     */
    String getName();

    /**
     * Returns the plugin version.
     */
    String getVersion();

    /**
     * Returns the encryption algorithm specification.
     */
    EncryptionAlgorithm getAlgorithm();

    /**
     * Encrypts plaintext asynchronously.
     *
     * @param plaintext the data to encrypt
     * @param key       the encryption key
     * @return CompletableFuture containing the encryption result
     */
    CompletableFuture<EncryptionResult> encrypt(byte[] plaintext, Key key);

    /**
     * Decrypts ciphertext asynchronously.
     *
     * @param ciphertext the data to decrypt
     * @param iv         the initialization vector (for symmetric encryption)
     * @param tag        the authentication tag (for GCM mode)
     * @param key        the decryption key
     * @return CompletableFuture containing the decrypted data
     */
    CompletableFuture<byte[]> decrypt(byte[] ciphertext, byte[] iv, byte[] tag, Key key);

    /**
     * Generates a new encryption key.
     *
     * @return CompletableFuture containing the new key
     */
    CompletableFuture<Key> generateKey();

    /**
     * Checks if the key is valid for this plugin.
     *
     * @param key the key to validate
     * @return true if the key is valid
     */
    boolean isKeyValid(Key key);
}
