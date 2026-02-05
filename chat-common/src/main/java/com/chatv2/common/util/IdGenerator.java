package com.chatv2.common.util;

import java.util.UUID;
import com.chatv2.common.exception.ValidationException;

/**
 * Utility class for generating unique identifiers.
 */
public final class IdGenerator {
    private IdGenerator() {
        // Utility class
    }

    /**
     * Generates a random UUID.
     */
    public static UUID generateUuid() {
        return UUID.randomUUID();
    }

    /**
     * Generates a UUID as string.
     */
    public static String generateUuidString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a UUID without dashes.
     */
    public static String generateShortUuidString() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Parses a UUID from string.
     */
    public static UUID parseUuid(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            throw new ValidationException("UUID string cannot be null or empty");
        }
        
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid UUID format: " + uuidString, e);
        }
    }

    /**
     * Validates a UUID string.
     */
    public static boolean isValidUuid(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return false;
        }
        
        // Check if the string has the correct UUID format (length and dash positions)
        if (!uuidString.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
            return false;
        }
        
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
