package com.chatv2.common.crypto;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Key manager for generating and managing cryptographic keys.
 */
public class KeyManager {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a random AES-256 key.
     */
    public static SecretKey generateAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, SECURE_RANDOM);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    /**
     * Generates a random AES key of specified size.
     */
    public static SecretKey generateAesKey(int keySizeBits) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(keySizeBits, SECURE_RANDOM);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    /**
     * Generates an RSA key pair.
     */
    public static KeyPair generateRsaKeyPair(int keySizeBits) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(keySizeBits, SECURE_RANDOM);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Generates a random IV for AES-GCM.
     */
    public static byte[] generateIv(int ivSizeBytes) {
        byte[] iv = new byte[ivSizeBytes];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Generates a random salt for password hashing.
     */
    public static byte[] generateSalt(int saltSizeBytes) {
        byte[] salt = new byte[saltSizeBytes];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Generates a random UUID.
     */
    public static UUID generateUuid() {
        return UUID.randomUUID();
    }

    /**
     * Encodes a key to Base64 string.
     */
    public static String encodeKeyToBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Decodes a key from Base64 string.
     */
    public static SecretKey decodeAesKeyFromBase64(String base64) {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Converts public key to Base64 encoded format.
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Converts Base64 string to public key.
     */
    public static PublicKey publicKeyFromBase64(String base64) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /**
     * Converts private key to Base64 encoded format.
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Converts Base64 string to private key.
     */
    public static PrivateKey privateKeyFromBase64(String base64) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Generates a secure random token for session authentication.
     */
    public static String generateSecureToken(int byteLength) {
        byte[] tokenBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Gets the secure random instance.
     */
    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }
}
