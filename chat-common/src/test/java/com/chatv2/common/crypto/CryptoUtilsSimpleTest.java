package com.chatv2.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CryptoUtils class.
 */
public class CryptoUtilsSimpleTest {

    @Test
    public void testEncryptAES() {
        // Test data
        String plaintext = "This is a test message";
        String key = "test-key-12345";
        
        // Encrypt
        String encrypted = com.chatv2.common.crypto.CryptoUtils.encryptAES(plaintext, key);
        
        // Verify
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted, "Encrypted text should be different from plaintext");
        
        // Null inputs
        assertDoesNotThrow(() -> {
            String result = com.chatv2.common.crypto.CryptoUtils.encryptAES(null, key);
            assertNull(result, "Should handle null plaintext");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            com.chatv2.common.crypto.CryptoUtils.encryptAES(plaintext, null);
        }, "Should throw exception for null key");
        
        assertThrows(IllegalArgumentException.class, () -> {
            com.chatv2.common.crypto.CryptoUtils.encryptAES(plaintext, "");
        }, "Should throw exception for blank key");
    }

    @Test
    public void testDecryptAES() {
        // Test data
        String plaintext = "This is a test message";
        String key = "test-key-12345";
        
        // Encrypt first
        String encrypted = com.chatv2.common.crypto.CryptoUtils.encryptAES(plaintext, key);
        
        // Decrypt
        String decrypted = com.chatv2.common.crypto.CryptoUtils.decryptAES(encrypted, key);
        
        // Verify
        assertEquals(plaintext, decrypted, "Decrypted text should match original plaintext");
        
        // Null inputs
        assertDoesNotThrow(() -> {
            String result = com.chatv2.common.crypto.CryptoUtils.decryptAES(null, key);
            assertNull(result, "Should handle null ciphertext");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            com.chatv2.common.crypto.CryptoUtils.decryptAES(encrypted, null);
        }, "Should throw exception for null key");
        
        assertThrows(IllegalArgumentException.class, () -> {
            com.chatv2.common.crypto.CryptoUtils.decryptAES(encrypted, "");
        }, "Should throw exception for blank key");
    }

    @Test
    public void testEncryptDecryptRoundTrip() {
        // Test various plaintexts
        String[] testTexts = {
            "Short",
            "A bit longer text with some special chars: !@#$%^&*()",
            "A very long text that might cause issues with some encryption algorithms. " +
            "It contains multiple sentences and various characters: 1234567890 and " +
            "some symbols like @#$%^&*()_+-={}[]|;:',.<>/?`~"
        };
        
        String key = "test-key-12345";
        
        for (String plaintext : testTexts) {
            // Encrypt
            String encrypted = com.chatv2.common.crypto.CryptoUtils.encryptAES(plaintext, key);
            
            // Decrypt
            String decrypted = com.chatv2.common.crypto.CryptoUtils.decryptAES(encrypted, key);
            
            // Verify round trip
            assertEquals(plaintext, decrypted, 
                String.format("Round trip failed for text: %s", plaintext));
        }
    }
}