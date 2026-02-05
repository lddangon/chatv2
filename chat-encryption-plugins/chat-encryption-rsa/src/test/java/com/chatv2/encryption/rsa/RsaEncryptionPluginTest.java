package com.chatv2.encryption.rsa;

import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.crypto.KeyManager;
import com.chatv2.encryption.api.EncryptionAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.security.Key;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaEncryptionPluginTest {

    private RsaEncryptionPlugin plugin;
    private KeyPair validKeyPair;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        // Create a new RSA plugin instance for each test
        plugin = new RsaEncryptionPlugin();
        
        // Generate a valid RSA key pair for testing
        validKeyPair = plugin.generateKeyPair().get();
    }

    @Test
    @DisplayName("Should return plugin name")
    void testGetName() {
        // When
        String name = plugin.getName();

        // Then
        assertThat(name).isEqualTo("RSA-4096");
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
        assertThat(algorithm.name()).isEqualTo("RSA-4096");
        assertThat(algorithm.transformation()).isEqualTo("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        assertThat(algorithm.keySize()).isEqualTo(4096);
        assertThat(algorithm.ivSize()).isEqualTo(0);
        assertThat(algorithm.tagSize()).isEqualTo(0);
        assertThat(algorithm.keyType()).isEqualTo(EncryptionAlgorithm.KeyType.ASYMMETRIC);
    }

    @Test
    @DisplayName("Should generate valid RSA key")
    void testGenerateKey() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<Key> futureKey = plugin.generateKey();
        Key key = futureKey.get();

        // Then
        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("RSA");
        assertThat(key.getEncoded()).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate valid RSA key pair")
    void testGenerateKeyPair() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<KeyPair> futureKeyPair = plugin.generateKeyPair();
        KeyPair keyPair = futureKeyPair.get();

        // Then
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isNotNull();
        assertThat(keyPair.getPrivate()).isNotNull();
        assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
        assertThat(keyPair.getPublic().getEncoded()).isNotEmpty();
        assertThat(keyPair.getPrivate().getEncoded()).isNotEmpty();
    }

    @Test
    @DisplayName("Should validate key correctly")
    void testIsKeyValid() {
        // Given
        Key rsaPublicKey = validKeyPair.getPublic();
        Key rsaPrivateKey = validKeyPair.getPrivate();
        
        Key invalidKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "AES";
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
        assertThat(plugin.isKeyValid(rsaPublicKey)).isTrue();
        assertThat(plugin.isKeyValid(rsaPrivateKey)).isTrue();
        assertThat(plugin.isKeyValid(invalidKey)).isFalse();
        assertThat(plugin.isKeyValid(null)).isFalse();
    }

    @Test
    @DisplayName("Should encrypt and decrypt correctly with key pair")
    void testEncryptDecryptWithKeyPair() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key publicKey = validKeyPair.getPublic();
        Key privateKey = validKeyPair.getPrivate();

        // When
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, publicKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        CompletableFuture<byte[]> decryptFuture = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                privateKey
        );
        byte[] decrypted = decryptFuture.get();

        // Then
        assertThat(encryptionResult).isNotNull();
        assertThat(encryptionResult.ciphertext()).isNotEmpty();
        assertThat(encryptionResult.iv()).isEmpty(); // RSA doesn't use IV
        assertThat(encryptionResult.tag()).isEmpty(); // RSA doesn't use tag
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Should encrypt correctly with empty data")
    void testEncryptEmptyData() throws ExecutionException, InterruptedException {
        // Given
        byte[] emptyPlaintext = new byte[0];
        Key publicKey = validKeyPair.getPublic();
        Key privateKey = validKeyPair.getPrivate();

        // When
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(emptyPlaintext, publicKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        CompletableFuture<byte[]> decryptFuture = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                privateKey
        );
        byte[] decrypted = decryptFuture.get();

        // Then
        assertThat(encryptionResult.ciphertext()).isNotEmpty();
        assertThat(decrypted).isEqualTo(emptyPlaintext);
    }

    @Test
    @DisplayName("Should encrypt and decrypt correctly with medium data")
    void testEncryptMediumData() throws ExecutionException, InterruptedException {
        // Given
        byte[] mediumPlaintext = new byte[200]; // RSA-4096 can handle up to ~501 bytes with OAEP padding
        for (int i = 0; i < mediumPlaintext.length; i++) {
            mediumPlaintext[i] = (byte) (i % 256);
        }
        Key publicKey = validKeyPair.getPublic();
        Key privateKey = validKeyPair.getPrivate();

        // When
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(mediumPlaintext, publicKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        CompletableFuture<byte[]> decryptFuture = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                privateKey
        );
        byte[] decrypted = decryptFuture.get();

        // Then
        assertThat(decrypted).isEqualTo(mediumPlaintext);
    }

    @Test
    @DisplayName("Should produce same ciphertext for same plaintext with same key")
    void testSameCiphertextForSamePlaintext() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key publicKey = validKeyPair.getPublic();

        // When
        CompletableFuture<EncryptionResult> encryptFuture1 = plugin.encrypt(plaintext, publicKey);
        CompletableFuture<EncryptionResult> encryptFuture2 = plugin.encrypt(plaintext, publicKey);
        
        EncryptionResult result1 = encryptFuture1.get();
        EncryptionResult result2 = encryptFuture2.get();

        // Then
        // RSA with OAEP padding uses random padding, so each encryption should be different
        assertThat(result1.ciphertext()).isNotEqualTo(result2.ciphertext());
    }

    @Test
    @DisplayName("Should throw exception when encrypting with invalid key")
    void testEncryptWithInvalidKey() {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key invalidKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "AES";
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
                .hasCauseInstanceOf(com.chatv2.common.crypto.exception.EncryptionException.class)
                .satisfies(ex -> {
                    Throwable cause = ex.getCause();
                    assertThat(cause.getCause().getMessage()).contains("Invalid RSA public key");
                });
    }

    @Test
    @DisplayName("Should throw exception when decrypting with invalid key")
    void testDecryptWithInvalidKey() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key publicKey = validKeyPair.getPublic();
        
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, publicKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        Key invalidKey = new Key() {
            @Override
            public String getAlgorithm() {
                return "AES";
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
                .hasCauseInstanceOf(com.chatv2.common.crypto.exception.DecryptionException.class)
                .satisfies(ex -> {
                    Throwable cause = ex.getCause();
                    assertThat(cause.getCause().getMessage()).contains("Invalid RSA private key");
                });
    }

    @Test
    @DisplayName("Should throw exception when decrypting tampered ciphertext")
    void testDecryptTamperedCiphertext() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key publicKey = validKeyPair.getPublic();
        Key privateKey = validKeyPair.getPrivate();
        
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, publicKey);
        EncryptionResult encryptionResult = encryptFuture.get();
        
        // Tamper with the ciphertext
        byte[] tamperedCiphertext = encryptionResult.ciphertext().clone();
        tamperedCiphertext[0] ^= 0x01; // Flip a bit

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                tamperedCiphertext,
                encryptionResult.iv(),
                encryptionResult.tag(),
                privateKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle concurrent encryption and decryption")
    void testConcurrentEncryptDecrypt() throws Exception {
        // Given
        int threadCount = 10;
        byte[] plaintext = "This is a secret message".getBytes();
        Key publicKey = validKeyPair.getPublic();
        Key privateKey = validKeyPair.getPrivate();
        
        // When
        CompletableFuture<?>[] futures = new CompletableFuture[threadCount]; // Encrypt and decrypt for each thread
        
        for (int i = 0; i < threadCount; i++) {
            // Encrypt
            CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, publicKey);
            futures[i] = encryptFuture.thenCompose(encryptionResult -> 
                // Decrypt
                plugin.decrypt(
                    encryptionResult.ciphertext(),
                    encryptionResult.iv(),
                    encryptionResult.tag(),
                    privateKey
                )
            );
        }
        
        // Then
        CompletableFuture.allOf(futures).get();
        
        for (int i = 0; i < threadCount; i++) {
            byte[] decrypted = (byte[]) futures[i].get();
            assertThat(decrypted).isEqualTo(plaintext);
        }
    }

    @Test
    @DisplayName("Should throw exception when encrypting with private key")
    void testEncryptWithPrivateKey() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key privateKey = validKeyPair.getPrivate();

        // When/Then
        CompletableFuture<EncryptionResult> future = plugin.encrypt(plaintext, privateKey);
        
        // RSA with OAEP padding cannot use private keys for encryption (only for signatures)
        // So this should throw an exception
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(com.chatv2.common.crypto.exception.EncryptionException.class)
                .extracting(Throwable::getCause)
                .extracting(Throwable::getCause)  // The cause of the cause (the actual InvalidKeyException)
                .extracting(Throwable::getMessage)
                .asString()
                .contains("OAEP cannot be used to sign or verify signatures");
    }

    @Test
    @DisplayName("Should throw exception when decrypting with public key")
    void testDecryptWithPublicKey() throws ExecutionException, InterruptedException {
        // Given
        byte[] plaintext = "This is a secret message".getBytes();
        Key publicKey = validKeyPair.getPublic();
        
        CompletableFuture<EncryptionResult> encryptFuture = plugin.encrypt(plaintext, publicKey);
        EncryptionResult encryptionResult = encryptFuture.get();

        // When/Then
        CompletableFuture<byte[]> future = plugin.decrypt(
                encryptionResult.ciphertext(),
                encryptionResult.iv(),
                encryptionResult.tag(),
                publicKey
        );
        
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}