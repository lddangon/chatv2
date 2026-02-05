package com.chatv2.server.gui.model;

import java.time.Duration;

/**
 * Record representing server statistics for GUI display.
 * Contains aggregated data about server performance and usage.
 *
 * @param userCount       Number of registered users
 * @param activeSessions  Number of currently active sessions
 * @param chatCount       Number of existing chats
 * @param messageCount    Total number of messages
 * @param messagesToday   Number of messages sent today
 * @param uptime          Server uptime duration
 */
public record ServerStatistics(
    int userCount,
    int activeSessions,
    int chatCount,
    int messageCount,
    int messagesToday,
    Duration uptime
) {
    /**
     * Creates a ServerStatistics instance with zero values.
     *
     * @return a ServerStatistics with all fields initialized to zero/empty
     */
    public static ServerStatistics empty() {
        return new ServerStatistics(0, 0, 0, 0, 0, Duration.ZERO);
    }

    /**
     * Gets the formatted uptime string.
     *
     * @return uptime in format "X days Y hours Z minutes"
     */
    public String getFormattedUptime() {
        long seconds = uptime.getSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
