package com.chatv2.encryption.rsa;

import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.crypto.KeyManager;
import com.chatv2.common.crypto.exception.DecryptionException;
import com.chatv2.common.crypto.exception.EncryptionException;
import com.chatv2.encryption.api.EncryptionAlgorithm;
import com.chatv2.encryption.api.EncryptionPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

/**
 * RSA-4096 encryption plugin implementation.
 */
public class RsaEncryptionPlugin implements EncryptionPlugin {
    private static final Logger log = LoggerFactory.getLogger(RsaEncryptionPlugin.class);
    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int KEY_SIZE = 4096; // bits

    private final EncryptionAlgorithm algorithm;

    public RsaEncryptionPlugin() {
        this.algorithm = EncryptionAlgorithm.rsa4096();
    }

    @Override
    public String getName() {
        return "RSA-4096";
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
                log.debug("Encrypting {} bytes with RSA-4096", plaintext.length);

                if (!isKeyValid(key)) {
                    throw new EncryptionException("Invalid RSA public key");
                }

                // Initialize cipher for encryption
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, key);

                // Encrypt plaintext
                byte[] ciphertext = cipher.doFinal(plaintext);

                log.debug("Encryption completed. Ciphertext: {} bytes", ciphertext.length);

                // RSA doesn't use IV or tag
                return new EncryptionResult(ciphertext, new byte[0], new byte[0]);
            } catch (Exception e) {
                log.error("Encryption failed", e);
                throw new EncryptionException("Failed to encrypt data with RSA-4096", e);
            }
        });
    }

    @Override
    public CompletableFuture<byte[]> decrypt(byte[] ciphertext, byte[] iv, byte[] tag, Key key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Decrypting {} bytes with RSA-4096", ciphertext.length);

                if (!isKeyValid(key)) {
                    throw new DecryptionException("Invalid RSA private key");
                }

                // Initialize cipher for decryption
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, key);

                // Decrypt ciphertext
                byte[] plaintext = cipher.doFinal(ciphertext);

                log.debug("Decryption completed. Plaintext: {} bytes", plaintext.length);

                return plaintext;
            } catch (Exception e) {
                log.error("Decryption failed", e);
                throw new DecryptionException("Failed to decrypt data with RSA-4096", e);
            }
        });
    }

    @Override
    public CompletableFuture<Key> generateKey() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating RSA-4096 key pair");
            KeyPair keyPair = KeyManager.generateRsaKeyPair(KEY_SIZE);
            log.debug("RSA-4096 key pair generated successfully");
            return keyPair.getPublic();
        });
    }

    @Override
    public boolean isKeyValid(Key key) {
        if (key == null) {
            return false;
        }
        return "RSA".equalsIgnoreCase(key.getAlgorithm()) && key.getEncoded().length > 0;
    }

    /**
     * Generates a complete RSA key pair.
     */
    public CompletableFuture<KeyPair> generateKeyPair() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Generating RSA-4096 key pair");
            KeyPair keyPair = KeyManager.generateRsaKeyPair(KEY_SIZE);
            log.debug("RSA-4096 key pair generated successfully");
            return keyPair;
        });
    }
}
