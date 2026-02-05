package com.chatv2.server.storage;

import com.chatv2.common.model.Message;
import com.chatv2.common.model.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class MessageRepositoryImpl implements MessageRepository {
    private static final Logger log = LoggerFactory.getLogger(MessageRepositoryImpl.class);
    private final Connection connection;

    private static final String INSERT_SQL = """
        INSERT INTO messages (message_id, chat_id, sender_id, content, message_type, reply_to_message_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE messages SET content = ?, edited_at = ? WHERE message_id = ?
        """;

    private static final String FIND_BY_ID_SQL = "SELECT * FROM messages WHERE message_id = ?";
    private static final String FIND_BY_CHAT_SQL = """
        SELECT * FROM messages WHERE chat_id = ?
        ORDER BY created_at DESC LIMIT ? OFFSET ?
        """;
    private static final String FIND_BEFORE_SQL = """
        SELECT * FROM messages WHERE chat_id = ? AND created_at <
        (SELECT created_at FROM messages WHERE message_id = ?)
        ORDER BY created_at DESC LIMIT ?
        """;
    private static final String FIND_UNREAD_SQL = """
        SELECT m.* FROM messages m
        LEFT JOIN message_read_receipts mrr ON m.message_id = mrr.message_id AND mrr.user_id = ?
        WHERE m.chat_id = ? AND mrr.user_id IS NULL
        ORDER BY m.created_at ASC
        """;
    private static final String FIND_BY_SENDER_SQL = """
        SELECT * FROM messages WHERE sender_id = ?
        ORDER BY created_at DESC LIMIT ?
        """;
    private static final String FIND_RECENT_SQL = """
        SELECT * FROM messages ORDER BY created_at DESC LIMIT ?
        """;
    private static final String ADD_READ_RECEIPT_SQL = """
        INSERT INTO message_read_receipts (receipt_id, message_id, user_id, read_at)
        VALUES (?, ?, ?, ?)
        """;
    private static final String GET_READ_RECEIPTS_SQL = """
        SELECT user_id FROM message_read_receipts WHERE message_id = ?
        """;
    private static final String DELETE_BY_CHAT_SQL = "DELETE FROM messages WHERE chat_id = ?";
    private static final String DELETE_SQL = "DELETE FROM messages WHERE message_id = ?";
    private static final String COUNT_BY_CHAT_SQL = "SELECT COUNT(*) FROM messages WHERE chat_id = ?";
    private static final String COUNT_ALL_SQL = "SELECT COUNT(*) FROM messages";
    private static final String COUNT_BY_USER_AFTER_DATE_SQL = """
        SELECT COUNT(*) FROM messages WHERE sender_id = ? AND created_at >= ?
        """;

    public MessageRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Message save(Message message) {
        try {
            // Check if message exists
            Optional<Message> existing = findById(message.messageId());
            if (existing.isPresent()) {
                // Update existing message
                PreparedStatement stmt = connection.prepareStatement(UPDATE_SQL);
                stmt.setString(1, message.content());
                stmt.setTimestamp(2, message.editedAt() != null ? Timestamp.from(message.editedAt()) : null);
                stmt.setObject(3, message.messageId());
                stmt.executeUpdate();
                stmt.close();
            } else {
                // Insert new message
                PreparedStatement stmt = connection.prepareStatement(INSERT_SQL);
                stmt.setObject(1, message.messageId());
                stmt.setObject(2, message.chatId());
                stmt.setObject(3, message.senderId());
                stmt.setString(4, message.content());
                stmt.setString(5, message.messageType().name());
                stmt.setObject(6, message.replyToMessageId());
                stmt.setTimestamp(7, Timestamp.from(message.createdAt()));
                stmt.executeUpdate();
                stmt.close();
            }
            return message;
        } catch (SQLException e) {
            log.error("Failed to save message", e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

    @Override
    public Optional<Message> findById(UUID messageId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID_SQL);
            stmt.setObject(1, messageId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToMessage(rs));
            }
            stmt.close();
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to find message by ID: {}", messageId, e);
            throw new RuntimeException("Failed to find message", e);
        }
    }

    @Override
    public List<Message> findMessagesByChat(UUID chatId, int limit, int offset) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_CHAT_SQL);
            stmt.setObject(1, chatId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            ResultSet rs = stmt.executeQuery();
            List<Message> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToMessage(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find messages for chat: {}", chatId, e);
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    @Override
    public List<Message> findMessagesBefore(UUID chatId, UUID beforeMessageId, int limit) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BEFORE_SQL);
            stmt.setObject(1, chatId);
            stmt.setObject(2, beforeMessageId);
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();
            List<Message> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToMessage(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find messages before: {}", beforeMessageId, e);
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    @Override
    public List<Message> findUnreadMessages(UUID chatId, UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_UNREAD_SQL);
            stmt.setObject(1, userId);
            stmt.setObject(2, chatId);
            ResultSet rs = stmt.executeQuery();
            List<Message> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToMessage(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find unread messages", e);
            throw new RuntimeException("Failed to find unread messages", e);
        }
    }

    @Override
    public List<Message> findMessagesBySender(UUID senderId, int limit) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_SENDER_SQL);
            stmt.setObject(1, senderId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            List<Message> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToMessage(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find messages by sender: {}", senderId, e);
            throw new RuntimeException("Failed to find messages", e);
        }
    }

    @Override
    public List<Message> findRecentMessages(int limit) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_RECENT_SQL);
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            List<Message> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToMessage(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find recent messages", e);
            throw new RuntimeException("Failed to find recent messages", e);
        }
    }

    @Override
    public void addReadReceipt(UUID messageId, UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(ADD_READ_RECEIPT_SQL);
            stmt.setObject(1, UUID.randomUUID());
            stmt.setObject(2, messageId);
            stmt.setObject(3, userId);
            stmt.setTimestamp(4, Timestamp.from(java.time.Instant.now()));
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to add read receipt", e);
            throw new RuntimeException("Failed to add read receipt", e);
        }
    }

    @Override
    public List<UUID> getReadReceipts(UUID messageId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(GET_READ_RECEIPTS_SQL);
            stmt.setObject(1, messageId);
            ResultSet rs = stmt.executeQuery();
            List<UUID> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getObject("user_id", UUID.class));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to get read receipts for message: {}", messageId, e);
            throw new RuntimeException("Failed to get read receipts", e);
        }
    }

    @Override
    public void deleteByChatId(UUID chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_BY_CHAT_SQL);
            stmt.setObject(1, chatId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to delete messages for chat: {}", chatId, e);
            throw new RuntimeException("Failed to delete messages", e);
        }
    }

    @Override
    public void delete(UUID messageId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_SQL);
            stmt.setObject(1, messageId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to delete message: {}", messageId, e);
            throw new RuntimeException("Failed to delete message", e);
        }
    }

    @Override
    public long countByChatId(UUID chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(COUNT_BY_CHAT_SQL);
            stmt.setObject(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                long count = rs.getLong(1);
                stmt.close();
                return count;
            }
            stmt.close();
            return 0;
        } catch (SQLException e) {
            log.error("Failed to count messages for chat: {}", chatId, e);
            throw new RuntimeException("Failed to count messages", e);
        }
    }

    @Override
    public int countAll() {
        try {
            PreparedStatement stmt = connection.prepareStatement(COUNT_ALL_SQL);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                stmt.close();
                return count;
            }
            stmt.close();
            return 0;
        } catch (SQLException e) {
            log.error("Failed to count all messages", e);
            throw new RuntimeException("Failed to count messages", e);
        }
    }

    @Override
    public int countByUserAfterDate(UUID userId, java.time.Instant date) {
        try {
            PreparedStatement stmt = connection.prepareStatement(COUNT_BY_USER_AFTER_DATE_SQL);
            stmt.setObject(1, userId);
            stmt.setTimestamp(2, Timestamp.from(date));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                stmt.close();
                return count;
            }
            stmt.close();
            return 0;
        } catch (SQLException e) {
            log.error("Failed to count messages for user {} after date {}", userId, date, e);
            throw new RuntimeException("Failed to count messages", e);
        }
    }

    private Message mapRowToMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getObject("message_id", UUID.class),
            rs.getObject("chat_id", UUID.class),
            rs.getObject("sender_id", UUID.class),
            rs.getString("content"),
            MessageType.fromString(rs.getString("message_type")),
            rs.getObject("reply_to_message_id", UUID.class),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("edited_at") != null ? rs.getTimestamp("edited_at").toInstant() : null,
            rs.getTimestamp("deleted_at") != null ? rs.getTimestamp("deleted_at").toInstant() : null,
            List.of() // Read receipts would need separate query
        );
    }
}
