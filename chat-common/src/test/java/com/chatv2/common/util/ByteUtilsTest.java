package com.chatv2.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ByteUtilsTest {

    @Test
    @DisplayName("Should convert int to byte array and back")
    void testIntToBytesAndBytesToInt() {
        // Given
        int intValue = 123456789;

        // When
        byte[] bytes = ByteUtils.intToBytes(intValue);
        int convertedBack = ByteUtils.bytesToInt(bytes);

        // Then
        assertThat(bytes).hasSize(4);
        assertThat(convertedBack).isEqualTo(intValue);
    }

    @Test
    @DisplayName("Should handle zero int value")
    void testZeroIntValue() {
        // Given
        int intValue = 0;

        // When
        byte[] bytes = ByteUtils.intToBytes(intValue);
        int convertedBack = ByteUtils.bytesToInt(bytes);

        // Then
        assertThat(bytes).hasSize(4);
        assertThat(convertedBack).isEqualTo(intValue);
        assertThat(bytes).containsExactly(0, 0, 0, 0);
    }

    @Test
    @DisplayName("Should handle maximum int value")
    void testMaxIntValue() {
        // Given
        int intValue = Integer.MAX_VALUE;

        // When
        byte[] bytes = ByteUtils.intToBytes(intValue);
        int convertedBack = ByteUtils.bytesToInt(bytes);

        // Then
        assertThat(convertedBack).isEqualTo(intValue);
    }

    @Test
    @DisplayName("Should handle minimum int value")
    void testMinIntValue() {
        // Given
        int intValue = Integer.MIN_VALUE;

        // When
        byte[] bytes = ByteUtils.intToBytes(intValue);
        int convertedBack = ByteUtils.bytesToInt(bytes);

        // Then
        assertThat(convertedBack).isEqualTo(intValue);
    }

    @Test
    @DisplayName("Should throw exception when converting bytes to int with insufficient data")
    void testBytesToIntWithInsufficientData() {
        // Given
        byte[] bytes = new byte[3];

        // When/Then
        assertThatThrownBy(() -> ByteUtils.bytesToInt(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Byte array must be at least 4 bytes");
    }

    @Test
    @DisplayName("Should convert long to byte array and back")
    void testLongToBytesAndBytesToLong() {
        // Given
        long longValue = 1234567890123456789L;

        // When
        byte[] bytes = ByteUtils.longToBytes(longValue);
        long convertedBack = ByteUtils.bytesToLong(bytes);

        // Then
        assertThat(bytes).hasSize(8);
        assertThat(convertedBack).isEqualTo(longValue);
    }

    @Test
    @DisplayName("Should handle zero long value")
    void testZeroLongValue() {
        // Given
        long longValue = 0L;

        // When
        byte[] bytes = ByteUtils.longToBytes(longValue);
        long convertedBack = ByteUtils.bytesToLong(bytes);

        // Then
        assertThat(bytes).hasSize(8);
        assertThat(convertedBack).isEqualTo(longValue);
        assertThat(bytes).containsExactly(0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    @DisplayName("Should handle maximum long value")
    void testMaxLongValue() {
        // Given
        long longValue = Long.MAX_VALUE;

        // When
        byte[] bytes = ByteUtils.longToBytes(longValue);
        long convertedBack = ByteUtils.bytesToLong(bytes);

        // Then
        assertThat(convertedBack).isEqualTo(longValue);
    }

    @Test
    @DisplayName("Should handle minimum long value")
    void testMinLongValue() {
        // Given
        long longValue = Long.MIN_VALUE;

        // When
        byte[] bytes = ByteUtils.longToBytes(longValue);
        long convertedBack = ByteUtils.bytesToLong(bytes);

        // Then
        assertThat(convertedBack).isEqualTo(longValue);
    }

    @Test
    @DisplayName("Should throw exception when converting bytes to long with insufficient data")
    void testBytesToLongWithInsufficientData() {
        // Given
        byte[] bytes = new byte[7];

        // When/Then
        assertThatThrownBy(() -> ByteUtils.bytesToLong(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Byte array must be at least 8 bytes");
    }

    @Test
    @DisplayName("Should concatenate multiple byte arrays")
    void testConcat() {
        // Given
        byte[] array1 = {1, 2, 3};
        byte[] array2 = {4, 5};
        byte[] array3 = {6, 7, 8, 9};

        // When
        byte[] result = ByteUtils.concat(array1, array2, array3);

        // Then
        assertThat(result).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    @DisplayName("Should handle concatenation with empty arrays")
    void testConcatWithEmptyArrays() {
        // Given
        byte[] array1 = {1, 2, 3};
        byte[] emptyArray = {};
        byte[] array2 = {4, 5};

        // When
        byte[] result = ByteUtils.concat(array1, emptyArray, array2);

        // Then
        assertThat(result).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Should handle concatenation with null arrays")
    void testConcatWithNullArrays() {
        // Given
        byte[] array1 = {1, 2, 3};
        byte[] nullArray = null;
        byte[] array2 = {4, 5};

        // When/Then
        assertThatThrownBy(() -> ByteUtils.concat(array1, nullArray, array2))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should extract sub-array")
    void testSubArray() {
        // Given
        byte[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        // When
        byte[] result = ByteUtils.subArray(array, 2, 5);

        // Then
        assertThat(result).containsExactly(3, 4, 5, 6, 7);
    }

    @Test
    @DisplayName("Should handle sub-array extraction with zero length")
    void testSubArrayZeroLength() {
        // Given
        byte[] array = {1, 2, 3, 4, 5};

        // When
        byte[] result = ByteUtils.subArray(array, 2, 0);

        // Then
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 10})
    @DisplayName("Should throw exception with invalid offset")
    void testSubArrayInvalidOffset(int offset) {
        // Given
        byte[] array = {1, 2, 3, 4, 5};

        // When/Then
        assertThatThrownBy(() -> ByteUtils.subArray(array, offset, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid offset or length");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 10})
    @DisplayName("Should throw exception with invalid length")
    void testSubArrayInvalidLength(int length) {
        // Given
        byte[] array = {1, 2, 3, 4, 5};

        // When/Then
        assertThatThrownBy(() -> ByteUtils.subArray(array, 2, length))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid offset or length");
    }

    @Test
    @DisplayName("Should compare byte arrays for equality")
    void testEquals() {
        // Given
        byte[] array1 = {1, 2, 3, 4, 5};
        byte[] array2 = {1, 2, 3, 4, 5};
        byte[] array3 = {1, 2, 3, 4, 6};

        // When/Then
        assertThat(ByteUtils.equals(array1, array2)).isTrue();
        assertThat(ByteUtils.equals(array1, array3)).isFalse();
        assertThat(ByteUtils.equals(array1, null)).isFalse();
        assertThat(ByteUtils.equals(null, array2)).isFalse();
        assertThat(ByteUtils.equals(null, null)).isTrue();
    }

    @Test
    @DisplayName("Should compare byte arrays in constant time")
    void testConstantTimeEquals() {
        // Given
        byte[] array1 = {1, 2, 3, 4, 5};
        byte[] array2 = {1, 2, 3, 4, 5};
        byte[] array3 = {1, 2, 3, 4, 6};

        // When/Then
        assertThat(ByteUtils.constantTimeEquals(array1, array2)).isTrue();
        assertThat(ByteUtils.constantTimeEquals(array1, array3)).isFalse();
    }

    @Test
    @DisplayName("Should return false for constant time comparison of arrays with different lengths")
    void testConstantTimeEqualsDifferentLengths() {
        // Given
        byte[] array1 = {1, 2, 3};
        byte[] array2 = {1, 2, 3, 4};

        // When/Then
        assertThat(ByteUtils.constantTimeEquals(array1, array2)).isFalse();
    }

    @Test
    @DisplayName("Should return true for constant time comparison of empty arrays")
    void testConstantTimeEqualsEmptyArrays() {
        // Given
        byte[] array1 = new byte[0];
        byte[] array2 = new byte[0];

        // When/Then
        assertThat(ByteUtils.constantTimeEquals(array1, array2)).isTrue();
    }

    @Test
    @DisplayName("Should convert byte array to hex string")
    void testToHex() {
        // Given
        byte[] bytes = {0x01, 0x23, (byte) 0xAB, (byte) 0xCD};

        // When
        String hex = ByteUtils.toHex(bytes);

        // Then
        assertThat(hex).isEqualTo("0123abcd");
    }

    @Test
    @DisplayName("Should convert hex string to byte array")
    void testFromHex() {
        // Given
        String hex = "0123abcd";

        // When
        byte[] bytes = ByteUtils.fromHex(hex);

        // Then
        assertThat(bytes).containsExactly(0x01, 0x23, (byte) 0xAB, (byte) 0xCD);
    }

    @Test
    @DisplayName("Should throw exception for hex string with odd length")
    void testFromHexOddLength() {
        // Given
        String hex = "123";

        // When/Then
        assertThatThrownBy(() -> ByteUtils.fromHex(hex))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Hex string must have even length");
    }

    @Test
    @DisplayName("Should handle hex conversion with empty string")
    void testFromHexEmptyString() {
        // Given
        String hex = "";

        // When
        byte[] bytes = ByteUtils.fromHex(hex);

        // Then
        assertThat(bytes).isEmpty();
    }

    @Test
    @DisplayName("Should convert byte array to string using UTF-8")
    void testToString() {
        // Given
        byte[] bytes = "Hello, world!".getBytes(StandardCharsets.UTF_8);

        // When
        String str = ByteUtils.toString(bytes);

        // Then
        assertThat(str).isEqualTo("Hello, world!");
    }

    @Test
    @DisplayName("Should convert string to byte array using UTF-8")
    void testFromString() {
        // Given
        String str = "Hello, world!";

        // When
        byte[] bytes = ByteUtils.fromString(str);

        // Then
        assertThat(bytes).isEqualTo(str.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should check if byte array is null or empty")
    void testIsEmpty() {
        // Given
        byte[] nullArray = null;
        byte[] emptyArray = {};
        byte[] nonEmptyArray = {1, 2, 3};

        // When/Then
        assertThat(ByteUtils.isEmpty(nullArray)).isTrue();
        assertThat(ByteUtils.isEmpty(emptyArray)).isTrue();
        assertThat(ByteUtils.isEmpty(nonEmptyArray)).isFalse();
    }

    @Test
    @DisplayName("Should reverse byte array in-place")
    void testReverse() {
        // Given
        byte[] array = {1, 2, 3, 4, 5};

        // When
        ByteUtils.reverse(array);

        // Then
        assertThat(array).containsExactly(5, 4, 3, 2, 1);
    }

    @Test
    @DisplayName("Should handle reversing array with single element")
    void testReverseSingleElement() {
        // Given
        byte[] array = {42};

        // When
        ByteUtils.reverse(array);

        // Then
        assertThat(array).containsExactly(42);
    }

    @Test
    @DisplayName("Should handle reversing empty array")
    void testReverseEmptyArray() {
        // Given
        byte[] array = {};

        // When
        ByteUtils.reverse(array);

        // Then
        assertThat(array).isEmpty();
    }

    @Test
    @DisplayName("Should return reversed copy of byte array")
    void testReversed() {
        // Given
        byte[] original = {1, 2, 3, 4, 5};

        // When
        byte[] reversed = ByteUtils.reversed(original);

        // Then
        assertThat(reversed).containsExactly(5, 4, 3, 2, 1);
        assertThat(original).containsExactly(1, 2, 3, 4, 5); // Original unchanged
    }

    @Test
    @DisplayName("Should ensure original array is unchanged by reversed method")
    void testReversedLeavesOriginalUnchanged() {
        // Given
        byte[] original = {1, 2, 3, 4, 5};

        // When
        byte[] reversed = ByteUtils.reversed(original);

        // Then
        assertThat(original).containsExactly(1, 2, 3, 4, 5);
        assertThat(reversed).isNotSameAs(original);
    }
}