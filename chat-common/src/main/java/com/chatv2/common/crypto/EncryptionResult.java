package com.chatv2.common.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Result of encryption operation containing ciphertext, IV, and tag.
 */
public record EncryptionResult(
    byte[] ciphertext,
    byte[] iv,
    byte[] tag
) {
    /**
     * Creates an EncryptionResult with empty IV and tag (for non-GCM modes).
     */
    public EncryptionResult(byte[] ciphertext) {
        this(ciphertext, new byte[0], new byte[0]);
    }

    /**
     * Creates an EncryptionResult for AES-GCM.
     */
    public EncryptionResult {
        if (ciphertext == null) {
            throw new IllegalArgumentException("ciphertext cannot be null");
        }
        if (iv == null) {
            throw new IllegalArgumentException("iv cannot be null");
        }
        if (tag == null) {
            throw new IllegalArgumentException("tag cannot be null");
        }
    }

    /**
     * Combines IV, tag, and ciphertext into single byte array.
     */
    public byte[] toCombinedArray() {
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + tag.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(tag);
        buffer.put(ciphertext);
        return buffer.array();
    }

    /**
     * Creates EncryptionResult from combined byte array.
     */
    public static EncryptionResult fromCombinedArray(byte[] combined, int ivSize, int tagSize) {
        if (combined.length < ivSize + tagSize) {
            throw new IllegalArgumentException("Combined array too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[ivSize];
        byte[] tag = new byte[tagSize];
        byte[] ciphertext = new byte[combined.length - ivSize - tagSize];

        buffer.get(iv);
        buffer.get(tag);
        buffer.get(ciphertext);

        return new EncryptionResult(ciphertext, iv, tag);
    }
}
