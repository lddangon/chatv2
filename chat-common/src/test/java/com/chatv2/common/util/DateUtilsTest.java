package com.chatv2.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateUtilsTest {

    @Test
    @DisplayName("Should return current time as Instant")
    void testNow() {
        // When
        Instant before = Instant.now();
        Instant now = DateUtils.now();
        Instant after = Instant.now();

        // Then
        assertThat(now).isBetween(before, after);
    }

    @Test
    @DisplayName("Should return current time as milliseconds")
    void testNowMillis() {
        // When
        long before = System.currentTimeMillis();
        long now = DateUtils.nowMillis();
        long after = System.currentTimeMillis();

        // Then
        assertThat(now).isBetween(before, after);
    }

    @Test
    @DisplayName("Should return current time as seconds")
    void testNowSeconds() {
        // When
        long before = System.currentTimeMillis() / 1000;
        long now = DateUtils.nowSeconds();
        long after = System.currentTimeMillis() / 1000;

        // Then
        assertThat(now).isBetween(before, after);
    }

    @Test
    @DisplayName("Should convert Instant to milliseconds")
    void testToMillis() {
        // Given
        Instant instant = Instant.ofEpochMilli(1234567890L);

        // When
        long millis = DateUtils.toMillis(instant);

        // Then
        assertThat(millis).isEqualTo(1234567890L);
    }

    @Test
    @DisplayName("Should convert milliseconds to Instant")
    void testFromMillis() {
        // Given
        long millis = 1234567890L;

        // When
        Instant instant = DateUtils.fromMillis(millis);

        // Then
        assertThat(instant.toEpochMilli()).isEqualTo(millis);
    }

    @Test
    @DisplayName("Should convert Instant to ISO string and back")
    void testToIsoStringAndFromIsoString() {
        // Given
        Instant instant = Instant.ofEpochSecond(1234567890);

        // When
        String isoString = DateUtils.toIsoString(instant);
        Instant parsed = DateUtils.fromIsoString(isoString);

        // Then
        assertThat(parsed).isEqualTo(instant);
        assertThat(isoString).isEqualTo(instant.toString());
    }

    @Test
    @DisplayName("Should throw exception for invalid ISO string")
    void testFromIsoStringWithInvalidString() {
        // Given
        String invalidIsoString = "invalid-date-string";

        // When/Then
        assertThatThrownBy(() -> DateUtils.fromIsoString(invalidIsoString))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid ISO-8601 format: " + invalidIsoString);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw exception for null ISO string")
    void testFromIsoStringWithNullString(String isoString) {
        // When/Then
        assertThatThrownBy(() -> DateUtils.fromIsoString(isoString))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should convert Instant to readable string")
    void testToReadableString() {
        // Given
        Instant instant = Instant.ofEpochSecond(1234567890);

        // When
        String readable = DateUtils.toReadableString(instant);

        // Then
        assertThat(readable).isNotNull();
        assertThat(readable).isNotBlank();
        
        // For more detailed verification, we can check that it's a valid date format
        ZonedDateTime parsed = ZonedDateTime.parse(readable, java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertThat(parsed.toInstant()).isEqualTo(instant);
    }

    @Test
    @DisplayName("Should detect past Instant")
    void testIsPast() {
        // Given
        Instant past = Instant.now().minusSeconds(60);
        Instant future = Instant.now().plusSeconds(60);

        // When/Then
        assertThat(DateUtils.isPast(past)).isTrue();
        assertThat(DateUtils.isPast(future)).isFalse();
    }

    @Test
    @DisplayName("Should detect future Instant")
    void testIsFuture() {
        // Given
        Instant past = Instant.now().minusSeconds(60);
        Instant future = Instant.now().plusSeconds(60);

        // When/Then
        assertThat(DateUtils.isFuture(past)).isFalse();
        assertThat(DateUtils.isFuture(future)).isTrue();
    }

    @Test
    @DisplayName("Should calculate duration between instants in seconds")
    void testDurationSeconds() {
        // Given
        Instant start = Instant.ofEpochSecond(1000);
        Instant end = Instant.ofEpochSecond(1500);

        // When
        long duration = DateUtils.durationSeconds(start, end);

        // Then
        assertThat(duration).isEqualTo(500);
    }

    @Test
    @DisplayName("Should calculate duration between instants in minutes")
    void testDurationMinutes() {
        // Given
        Instant start = Instant.ofEpochSecond(0);
        Instant end = Instant.ofEpochSecond(300); // 5 minutes

        // When
        long duration = DateUtils.durationMinutes(start, end);

        // Then
        assertThat(duration).isEqualTo(5);
    }

    @Test
    @DisplayName("Should calculate duration between instants in hours")
    void testDurationHours() {
        // Given
        Instant start = Instant.ofEpochSecond(0);
        Instant end = Instant.ofEpochSecond(7200); // 2 hours

        // When
        long duration = DateUtils.durationHours(start, end);

        // Then
        assertThat(duration).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle negative duration (end before start)")
    void testNegativeDuration() {
        // Given
        Instant start = Instant.ofEpochSecond(1000);
        Instant end = Instant.ofEpochSecond(500);

        // When
        long durationSeconds = DateUtils.durationSeconds(start, end);
        long durationMinutes = DateUtils.durationMinutes(start, end);
        long durationHours = DateUtils.durationHours(start, end);

        // Then
        assertThat(durationSeconds).isNegative();
        // Minutes and hours might be 0 if the difference is less than a minute/hour
        assertThat(durationMinutes).isLessThanOrEqualTo(0);
        assertThat(durationHours).isLessThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should add seconds to Instant")
    void testAddSeconds() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);
        long secondsToAdd = 500;

        // When
        Instant result = DateUtils.addSeconds(instant, secondsToAdd);

        // Then
        assertThat(result.getEpochSecond()).isEqualTo(1500);
    }

    @Test
    @DisplayName("Should add minutes to Instant")
    void testAddMinutes() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);
        long minutesToAdd = 5;

        // When
        Instant result = DateUtils.addMinutes(instant, minutesToAdd);

        // Then
        assertThat(result.getEpochSecond()).isEqualTo(1300); // 5 * 60 = 300 seconds
    }

    @Test
    @DisplayName("Should add hours to Instant")
    void testAddHours() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);
        long hoursToAdd = 2;

        // When
        Instant result = DateUtils.addHours(instant, hoursToAdd);

        // Then
        assertThat(result.getEpochSecond()).isEqualTo(8200); // 2 * 3600 = 7200 seconds
    }

    @Test
    @DisplayName("Should add days to Instant")
    void testAddDays() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);
        long daysToAdd = 3;

        // When
        Instant result = DateUtils.addDays(instant, daysToAdd);

        // Then
        assertThat(result.getEpochSecond()).isEqualTo(260200); // 1000 + 3 * 86400 = 260200 seconds
    }

    @Test
    @DisplayName("Should handle adding negative values")
    void testAddNegativeValues() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);

        // When
        Instant minusSeconds = DateUtils.addSeconds(instant, -200);
        Instant minusMinutes = DateUtils.addMinutes(instant, -5);
        Instant minusHours = DateUtils.addHours(instant, -1);
        Instant minusDays = DateUtils.addDays(instant, -1);

        // Then
        assertThat(minusSeconds.getEpochSecond()).isEqualTo(800);
        assertThat(minusMinutes.getEpochSecond()).isEqualTo(700); // -5 * 60 = -300 seconds
        assertThat(minusHours.getEpochSecond()).isEqualTo(-2600); // -1 * 3600 = -3600 seconds
        assertThat(minusDays.getEpochSecond()).isEqualTo(-85400); // -1 * 86400 = -86400 seconds
    }

    @Test
    @DisplayName("Should handle adding zero values")
    void testAddZeroValues() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);

        // When
        Instant resultSeconds = DateUtils.addSeconds(instant, 0);
        Instant resultMinutes = DateUtils.addMinutes(instant, 0);
        Instant resultHours = DateUtils.addHours(instant, 0);
        Instant resultDays = DateUtils.addDays(instant, 0);

        // Then
        assertThat(resultSeconds).isEqualTo(instant);
        assertThat(resultMinutes).isEqualTo(instant);
        assertThat(resultHours).isEqualTo(instant);
        assertThat(resultDays).isEqualTo(instant);
    }

    @Test
    @DisplayName("Should handle chaining date operations")
    void testChainOperations() {
        // Given
        Instant instant = Instant.ofEpochSecond(1000);

        // When
        Instant result = DateUtils.addDays(
                DateUtils.addHours(
                        DateUtils.addMinutes(
                                DateUtils.addSeconds(instant, 30),
                                5),
                        2),
                1);

        // Then
        // 30 seconds + 5 minutes (300 seconds) + 2 hours (7200 seconds) + 1 day (86400 seconds) = 93930 seconds
        assertThat(result.getEpochSecond()).isEqualTo(94930);
    }
}