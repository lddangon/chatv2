package com.chatv2.common.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 */
public final class DateUtils {
    private DateUtils() {
        // Utility class
    }

    /**
     * Gets current time as Instant.
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Gets current time as Unix epoch milliseconds.
     */
    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Gets current time as Unix epoch seconds.
     */
    public static long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Converts Instant to Unix epoch milliseconds.
     */
    public static long toMillis(Instant instant) {
        return instant.toEpochMilli();
    }

    /**
     * Converts Unix epoch milliseconds to Instant.
     */
    public static Instant fromMillis(long millis) {
        return Instant.ofEpochMilli(millis);
    }

    /**
     * Formats Instant as ISO-8601 string.
     */
    public static String toIsoString(Instant instant) {
        return instant.toString();
    }

    /**
     * Parses ISO-8601 string to Instant.
     */
    public static Instant fromIsoString(String isoString) {
        try {
            return Instant.parse(isoString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ISO-8601 format: " + isoString, e);
        }
    }

    /**
     * Formats Instant as readable date string.
     */
    public static String toReadableString(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        return zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    /**
     * Checks if Instant is in the past.
     */
    public static boolean isPast(Instant instant) {
        return instant.isBefore(Instant.now());
    }

    /**
     * Checks if Instant is in the future.
     */
    public static boolean isFuture(Instant instant) {
        return instant.isAfter(Instant.now());
    }

    /**
     * Calculates duration between two instants in seconds.
     */
    public static long durationSeconds(Instant start, Instant end) {
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * Calculates duration between two instants in minutes.
     */
    public static long durationMinutes(Instant start, Instant end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Calculates duration between two instants in hours.
     */
    public static long durationHours(Instant start, Instant end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Adds seconds to an Instant.
     */
    public static Instant addSeconds(Instant instant, long seconds) {
        return instant.plusSeconds(seconds);
    }

    /**
     * Adds minutes to an Instant.
     */
    public static Instant addMinutes(Instant instant, long minutes) {
        return instant.plus(Duration.ofMinutes(minutes));
    }

    /**
     * Adds hours to an Instant.
     */
    public static Instant addHours(Instant instant, long hours) {
        return instant.plus(Duration.ofHours(hours));
    }

    /**
     * Adds days to an Instant.
     */
    public static Instant addDays(Instant instant, long days) {
        return instant.plus(Duration.ofDays(days));
    }
}
