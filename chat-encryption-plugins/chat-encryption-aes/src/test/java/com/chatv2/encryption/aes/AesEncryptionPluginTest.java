package com.chatv2.encryption.aes;

import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.crypto.KeyManager;
import com.chatv2.encryption.api.EncryptionAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEncryptionPluginTest {

    private AesEncryptionPlugin plugin;
    private Key validKey;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        // Create a new AES plugin instance for each test
        plugin = new AesEncryptionPlugin();

        // Generate a valid AES key for testing
        validKey = plugin.generateKey().get();
    }

    @Test
    @DisplayName("Should return plugin name")
    void testGetName() {
        // When
        String name = plugin.getName();

        // Then
        assertThat(name).isEqualTo("AES-256-GCM");
    }

    @Test
    @DisplayName("Should return plugin version")
    void testGetVersion() {
        // When
        String version = plugin.getVersion();

        // Then
        assertThat(version).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("Should return encryption algorithm")
    void testGetAlgorithm() {
        // When
        EncryptionAlgorithm algorithm = plugin.getAlgorithm();

        // Then
        assertThat(algorithm.name()).isEqualTo("AES-256-GCM");
        assertThat(algorithm.transformation()).isEqualTo("AES/GCM/NoPadding");
        assertThat(algorithm.keySize()).isEqualTo(256);
        assertThat(algorithm.ivSize()).isEqualTo(12);
        assertThat(algorithm.tagSize()).isEqualTo(16);
        assertThat(algorithm.keyType()).isEqualTo(EncryptionAlgorithm.KeyType.SYMMETRIC);
    }

    @Test
    @DisplayName("Should generate valid AES key")
    void testGenerateKey() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<Key> futureKey = plugin.generateKey();
        Key key = futureKey.get();

        // Then
        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("AES");
        assertThat(key.getEncoded()).hasSize(32); // 256 bits = 32 bytes
    }

    @Test
    @DisplayName("Should validate key correctly")
    void testIsKeyValid() {
        // Given
        SecretKey aesKey = KeyManager.generateAesKey(256);
        Key invalidKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "RSA";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[32];
            }
        };

        // When/Then
        assertThat(plugin.isKeyValid(aesKey)).isTrue();
        assertThat(plugin.isKeyValid(invalidKey)).isFalse();
        assertThat(plugin.isKeyValid(null)).isFalse();
    }

    @Test
    @DisplayName("Should encrypt and decrypt correctly")
    void testEncryptDecrypt() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();

        // When
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        CompletableFuture<byte[]> decryptFuture = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                validKey
        );
        byte[] decrypted = decryptFuture.get();

        // Then
        assertThat(encryptionResult).isNotNull();
        assertThat(encryptionResult.ciphertext()).isNotEmpty();
        assertThat(encryptionResult.iv()).isNotEmpty();
        assertThat(encryptionResult.iv()).hasSize(12); // IV size for AES-GCM
        assertThat(encryptionResult.tag()).isNotEmpty();
        assertThat(encryptionResult.tag()).hasSize(16); // Tag size for AES-GCM
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Should encrypt correctly with empty data")
    void testEncryptEmptyData() throws ExecutionException, InterruptedException {
        // Given
        byte[] emptyPlaintext = new byte[0];

        // When
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(emptyPlaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();

        CompletableFuture<byte[]> decryptFuture = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                validKey
        );
        byte[] decrypted = decryptFuture.get();

        // Then
        // AES-GCM produces empty ciphertext for empty plaintext (only the tag is generated)
        assertThat(encryptionResult.ciphertext()).isEmpty();
        assertThat(encryptionResult.iv()).isNotEmpty();
        assertThat(encryptionResult.iv()).hasSize(12);
        assertThat(encryptionResult.tag()).isNotEmpty();
        assertThat(encryptionResult.tag()).hasSize(16);
        assertThat(decrypted).isEqualTo(emptyPlaintext);
    }

    @Test
    @DisplayName("Should encrypt correctly with large data")
    void testEncryptLargeData() throws ExecutionException, InterruptedException {
        // Given
        byte[] largePlaintext = new byte[1024 * 1024]; // 1 MB
        for (int i = 0; i < largePlaintext.length; i++) {
            largePlaintext[i] = (byte) (i % 256);
        }

        // When
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(largePlaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        CompletableFuture<byte[]> decryptFuture = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                validKey
        );
        byte[] decrypted = decryptFuture.get();

        // Then
        assertThat(decrypted).isEqualTo(largePlaintext);
    }

    @Test
    @DisplayName("Should produce different ciphertext for same plaintext with different IV")
    void testDifferentCiphertextForSamePlaintext() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();

        // When
        CompletableFuture<EncryptionResult> encryptFuture1 = plugin.encrypt(plaintext, validKey);
        CompletableFuture<EncryptionResult> encryptFuture2 = plugin.encrypt(plaintext, validKey);
        
        EncryptionResult result1 = encryptFuture1.get();
        EncryptionResult result2 = encryptFuture2.get();

        // Then
        assertThat(result1.ciphertext()).isNotEqualTo(result2.ciphertext());
        assertThat(result1.iv()).isNotEqualTo(result2.iv());
        assertThat(result1.tag()).isNotEqualTo(result2.tag());
    }

    @Test
    @DisplayName("Should throw exception when encrypting with invalid key")
    void testEncryptWithInvalidKey() {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key invalidKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "RSA";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[32];
            }
        };

        // When/Then
        CompletableFuture<EncryptionResult> future = plugin.encrypt(plaintext, invalidKey);
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid AES key");
    }

    @Test
    @DisplayName("Should throw exception when decrypting with invalid key")
    void testDecryptWithInvalidKey() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        Key invalidKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "RSA";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[32];
            }
        };

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                invalidKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid AES key");
    }

    @Test
    @DisplayName("Should throw exception when decrypting with invalid IV length")
    void testDecryptWithInvalidIvLength() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        byte[] invalidIv = new byte[16]; // Wrong length, should be 12

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                encryptionResult.ciphertext(),
                invalidIv,
                encryptionResult.tag(),
                validKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid IV length");
    }

    @Test
    @DisplayName("Should throw exception when decrypting with invalid tag length")
    void testDecryptWithInvalidTagLength() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        byte[] invalidTag = new byte[12]; // Wrong length, should be 16

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                invalidTag,
                validKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid tag length");
    }

    @Test
    @DisplayName("Should throw exception when decrypting tampered ciphertext")
    void testDecryptTamperedCiphertext() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        // Tamper with the ciphertext
        byte[] tamperedCiphertext = encryptionResult.ciphertext().clone();
        tamperedCiphertext[0] ^= 0x01; // Flip a bit

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                tamperedCiphertext,
                encryptionResult.iv(),
                encryptionResult.tag(),
                validKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should throw exception when decrypting with null IV")
    void testDecryptWithNullIv() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        byte[] nullIv = null;

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                encryptionResult.ciphertext(),
                nullIv,
                encryptionResult.tag(),
                validKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid IV length");
    }

    @Test
    @DisplayName("Should throw exception when decrypting with null tag")
    void testDecryptWithNullTag() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, validKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        byte[] nullTag = null;

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                nullTag,
                validKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid tag length");
    }

    @Test
    @DisplayName("Should handle concurrent encryption and decryption")
    void testConcurrentEncryptDecrypt() throws Exception {
        // Given
        int threadCount = 10;
        byte[] plaintext = "This is a secret message".getBytes();

        // When
        @SuppressWarnings("unchecked")
        CompletableFuture<byte[]>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            // Encrypt then decrypt in chain
            futures[i] = plugin.encrypt(plaintext, validKey)
                .thenCompose(encryptionResult ->
                    plugin.decrypt(
                        encryptionResult.ciphertext(),
                        encryptionResult.iv(),
                        encryptionResult.tag(),
                        validKey
                    )
                );
        }

        // Then
        CompletableFuture.allOf(futures).get();

        for (int i = 0; i < threadCount; i++) {
            byte[] decrypted = futures[i].get();
            assertThat(decrypted).isEqualTo(plaintext);
        }
    }
}