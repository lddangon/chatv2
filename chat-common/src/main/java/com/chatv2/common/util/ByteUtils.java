package com.chatv2.common.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Utility class for byte array operations.
 */
public final class ByteUtils {
    private ByteUtils() {
        // Utility class
    }

    /**
     * Converts an int to byte array (big-endian).
     */
    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(value)
            .array();
    }

    /**
     * Converts a byte array to int (big-endian).
     */
    public static int bytesToInt(byte[] bytes) {
        if (bytes.length < 4) {
            throw new IllegalArgumentException("Byte array must be at least 4 bytes");
        }
        return ByteBuffer.wrap(bytes, 0, 4)
            .order(ByteOrder.BIG_ENDIAN)
            .getInt();
    }

    /**
     * Converts a long to byte array (big-endian).
     */
    public static byte[] longToBytes(long value) {
        return ByteBuffer.allocate(8)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(value)
            .array();
    }

    /**
     * Converts a byte array to long (big-endian).
     */
    public static long bytesToLong(byte[] bytes) {
        if (bytes.length < 8) {
            throw new IllegalArgumentException("Byte array must be at least 8 bytes");
        }
        return ByteBuffer.wrap(bytes, 0, 8)
            .order(ByteOrder.BIG_ENDIAN)
            .getLong();
    }

    /**
     * Concatenates multiple byte arrays.
     */
    public static byte[] concat(byte[]... arrays) {
        int totalLength = Arrays.stream(arrays).mapToInt(arr -> arr.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        for (byte[] array : arrays) {
            buffer.put(array);
        }
        return buffer.array();
    }

    /**
     * Extracts a sub-array from byte array.
     */
    public static byte[] subArray(byte[] array, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > array.length) {
            throw new IllegalArgumentException("Invalid offset or length");
        }
        return Arrays.copyOfRange(array, offset, offset + length);
    }

    /**
     * Compares two byte arrays for equality.
     */
    public static boolean equals(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }

    /**
     * Constant-time byte array comparison to prevent timing attacks.
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
     * Converts byte array to hex string.
     */
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts hex string to byte array.
     */
    public static byte[] fromHex(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * Converts byte array to string using UTF-8.
     */
    public static String toString(byte[] bytes) {
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Converts string to byte array using UTF-8.
     */
    public static byte[] fromString(String str) {
        return str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Checks if byte array is null or empty.
     */
    public static boolean isEmpty(byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    /**
     * Reverses byte array in-place.
     */
    public static void reverse(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
    }

    /**
     * Reverses a copy of byte array.
     */
    public static byte[] reversed(byte[] array) {
        byte[] reversed = array.clone();
        reverse(reversed);
        return reversed;
    }
}
