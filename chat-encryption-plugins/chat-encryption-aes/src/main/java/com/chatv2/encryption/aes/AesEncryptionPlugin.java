package com.chatv2.encryption.aes;

import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.crypto.KeyManager;
import com.chatv2.common.crypto.exception.DecryptionException;
import com.chatv2.common.crypto.exception.EncryptionException;
import com.chatv2.encryption.api.EncryptionAlgorithm;
import com.chatv2.encryption.api.EncryptionPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;

/**
 * AES-256-GCM encryption plugin implementation.
 */
public class AesEncryptionPlugin implements EncryptionPlugin {
    private static final Logger log = LoggerFactory.getLogger(AesEncryptionPlugin.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // bits
    private static final int IV_SIZE = 12; // bytes (96 bits)
    private static final int TAG_SIZE = 16; // bytes (128 bits)
    private static final int TAG_BIT_LENGTH = TAG_SIZE * 8;

    private final EncryptionAlgorithm algorithm;

    public AesEncryptionPlugin() {
        this.algorithm = EncryptionAlgorithm.aes256Gcm();
    }

    @Override
    public String getName() {
        return "AES-256-GCM";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public EncryptionAlgorithm getAlgorithm() {
        return algorithm;
    }

    @Override
    public CompletableFuture<EncryptionResult> encrypt(byte[] plaintext, Key key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Encrypting {} bytes with AES-256-GCM", plaintext.length);

                if (!isKeyValid(key)) {
                    throw new EncryptionException("Invalid AES key");
                }

                // Generate random IV
                byte[] iv = new byte[IV_SIZE];
                SECURE_RANDOM.nextBytes(iv);

                // Initialize cipher for encryption
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
                cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

                // Encrypt plaintext
                byte[] ciphertext = cipher.doFinal(plaintext);

                // Extract tag from ciphertext (last 16 bytes)
                byte[] tag = new byte[TAG_SIZE];
                byte[] encryptedData = new byte[ciphertext.length - TAG_SIZE];
                System.arraycopy(ciphertext, 0, encryptedData, 0, encryptedData.length);
                System.arraycopy(ciphertext, encryptedData.length, tag, 0, TAG_SIZE);

                log.debug("Encryption completed. Ciphertext: {} bytes, IV: {} bytes, Tag: {} bytes",
                    encryptedData.length, iv.length, tag.length);

                return new EncryptionResult(encryptedData, iv, tag);
            } catch (EncryptionException e) {
                log.error("Encryption failed: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Encryption failed", e);
                throw new EncryptionException("Failed to encrypt data with AES-256-GCM", e);
            }
        });
    }

    @Override
    public CompletableFuture<byte[]> decrypt(byte[] ciphertext, byte[] iv, byte[] tag, Key key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Decrypting {} bytes with AES-256-GCM", ciphertext.length);

                if (!isKeyValid(key)) {
                    throw new DecryptionException("Invalid AES key");
                }
                if (iv == null || iv.length != IV_SIZE) {
                    throw new DecryptionException("Invalid IV length");
                }
                if (tag == null || tag.length != TAG_SIZE) {
                    throw new DecryptionException("Invalid tag length");
                }

                // Combine ciphertext and tag
                byte[] combinedCiphertext = new byte[ciphertext.length + tag.length];
                System.arraycopy(ciphertext, 0, combinedCiphertext, 0, ciphertext.length);
                System.arraycopy(tag, 0, combinedCiphertext, ciphertext.length, tag.length);

                // Initialize cipher for decryption
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
                cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

                // Decrypt ciphertext
                byte[] plaintext = cipher.doFinal(combinedCiphertext);

                log.debug("Decryption completed. Plaintext: {} bytes", plaintext.length);

                return plaintext;
            } catch (DecryptionException e) {
                log.error("Decryption failed: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Decryption failed", e);
                throw new DecryptionException("Failed to decrypt data with AES-256-GCM", e);
            }
        });
    }

    @Override
    public CompletableFuture<Key> generateKey() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating AES-256 key");
            SecretKey secretKey = KeyManager.generateAesKey(KEY_SIZE);
            log.debug("AES-256 key generated successfully");
            return secretKey;
        });
    }

    @Override
    public boolean isKeyValid(Key key) {
        if (key == null) {
            return false;
        }
        return ALGORITHM.equalsIgnoreCase(key.getAlgorithm()) && key.getEncoded().length == KEY_SIZE / 8;
    }
}
