package com.chatv2.server.storage;

import com.chatv2.common.model.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class ChatRepositoryImpl implements ChatRepository {
    private static final Logger log = LoggerFactory.getLogger(ChatRepositoryImpl.class);
    private final Connection connection;

    private static final String INSERT_SQL = """
        INSERT INTO chats (chat_id, chat_type, name, description, owner_id, avatar_data, created_at, updated_at, participant_count)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE chats SET name = ?, description = ?, avatar_data = ?, updated_at = ?, participant_count = ?
        WHERE chat_id = ?
        """;

    private static final String FIND_BY_ID_SQL = "SELECT * FROM chats WHERE chat_id = ?";
    private static final String FIND_BY_USER_SQL = """
        SELECT DISTINCT c.* FROM chats c
        INNER JOIN chat_participants cp ON c.chat_id = cp.chat_id
        WHERE cp.user_id = ?
        ORDER BY c.updated_at DESC
        """;
    private static final String ADD_PARTICIPANT_SQL = """
        INSERT INTO chat_participants (id, chat_id, user_id, role) VALUES (?, ?, ?, ?)
        """;
    private static final String REMOVE_PARTICIPANT_SQL = """
        DELETE FROM chat_participants WHERE chat_id = ? AND user_id = ?
        """;
    private static final String FIND_PARTICIPANTS_SQL = """
        SELECT user_id FROM chat_participants WHERE chat_id = ?
        """;
    private static final String GET_PARTICIPANT_COUNT_SQL = """
        SELECT COUNT(*) FROM chat_participants WHERE chat_id = ?
        """;
    private static final String IS_PARTICIPANT_SQL = """
        SELECT COUNT(*) FROM chat_participants WHERE chat_id = ? AND user_id = ?
        """;
    private static final String COUNT_ALL_SQL = "SELECT COUNT(*) FROM chats";
    private static final String FIND_ALL_SQL = "SELECT * FROM chats";
    private static final String DELETE_SQL = "DELETE FROM chats WHERE chat_id = ?";

    public ChatRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Chat save(Chat chat) {
        try {
            // Check if chat exists
            Optional<Chat> existing = findById(chat.chatId());
            if (existing.isPresent()) {
                // Update existing chat
                PreparedStatement stmt = connection.prepareStatement(UPDATE_SQL);
                stmt.setString(1, chat.name());
                stmt.setString(2, chat.description());
                stmt.setString(3, chat.avatarData());
                stmt.setTimestamp(4, Timestamp.from(chat.updatedAt()));
                stmt.setInt(5, chat.participantCount());
                stmt.setObject(6, chat.chatId());
                stmt.executeUpdate();
                stmt.close();
            } else {
                // Insert new chat
                PreparedStatement stmt = connection.prepareStatement(INSERT_SQL);
                stmt.setObject(1, chat.chatId());
                stmt.setString(2, chat.chatType().name());
                stmt.setString(3, chat.name());
                stmt.setString(4, chat.description());
                stmt.setObject(5, chat.ownerId());
                stmt.setString(6, chat.avatarData());
                stmt.setTimestamp(7, Timestamp.from(chat.createdAt()));
                stmt.setTimestamp(8, Timestamp.from(chat.updatedAt()));
                stmt.setInt(9, chat.participantCount());
                stmt.executeUpdate();
                stmt.close();
            }
            return chat;
        } catch (SQLException e) {
            log.error("Failed to save chat", e);
            throw new RuntimeException("Failed to save chat", e);
        }
    }

    @Override
    public Optional<Chat> findById(UUID chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_ID_SQL);
            stmt.setObject(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToChat(rs));
            }
            stmt.close();
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Failed to find chat by ID: {}", chatId, e);
            throw new RuntimeException("Failed to find chat", e);
        }
    }

    @Override
    public List<Chat> findByUser(UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_BY_USER_SQL);
            stmt.setObject(1, userId);
            ResultSet rs = stmt.executeQuery();
            List<Chat> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToChat(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find chats for user: {}", userId, e);
            throw new RuntimeException("Failed to find chats", e);
        }
    }

    @Override
    public List<Chat> findPrivateChats(UUID user1Id, UUID user2Id) {
        try {
            String sql = """
                SELECT DISTINCT c.* FROM chats c
                INNER JOIN chat_participants cp1 ON c.chat_id = cp1.chat_id
                INNER JOIN chat_participants cp2 ON c.chat_id = cp2.chat_id
                WHERE c.chat_type = 'PRIVATE'
                AND cp1.user_id = ? AND cp2.user_id = ?
                AND cp1.user_id != cp2.user_id
                """;
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setObject(1, user1Id);
            stmt.setObject(2, user2Id);
            ResultSet rs = stmt.executeQuery();
            List<Chat> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToChat(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find private chats", e);
            throw new RuntimeException("Failed to find private chats", e);
        }
    }

    @Override
    public List<Chat> findGroupChats() {
        try {
            String sql = "SELECT * FROM chats WHERE chat_type = 'GROUP'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            List<Chat> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToChat(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find group chats", e);
            throw new RuntimeException("Failed to find group chats", e);
        }
    }

    @Override
    public void addParticipant(UUID chatId, UUID userId, String role) {
        try {
            PreparedStatement stmt = connection.prepareStatement(ADD_PARTICIPANT_SQL);
            stmt.setObject(1, UUID.randomUUID());
            stmt.setObject(2, chatId);
            stmt.setObject(3, userId);
            stmt.setString(4, role);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to add participant to chat", e);
            throw new RuntimeException("Failed to add participant", e);
        }
    }

    @Override
    public void removeParticipant(UUID chatId, UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(REMOVE_PARTICIPANT_SQL);
            stmt.setObject(1, chatId);
            stmt.setObject(2, userId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to remove participant from chat", e);
            throw new RuntimeException("Failed to remove participant", e);
        }
    }

    @Override
    public Set<UUID> findParticipants(UUID chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_PARTICIPANTS_SQL);
            stmt.setObject(1, chatId);
            ResultSet rs = stmt.executeQuery();
            Set<UUID> participants = new HashSet<>();
            while (rs.next()) {
                participants.add(rs.getObject("user_id", UUID.class));
            }
            stmt.close();
            return participants;
        } catch (SQLException e) {
            log.error("Failed to find participants for chat: {}", chatId, e);
            throw new RuntimeException("Failed to find participants", e);
        }
    }

    @Override
    public int getParticipantCount(UUID chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(GET_PARTICIPANT_COUNT_SQL);
            stmt.setObject(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                stmt.close();
                return count;
            }
            stmt.close();
            return 0;
        } catch (SQLException e) {
            log.error("Failed to get participant count for chat: {}", chatId, e);
            throw new RuntimeException("Failed to get participant count", e);
        }
    }

    @Override
    public boolean isParticipant(UUID chatId, UUID userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(IS_PARTICIPANT_SQL);
            stmt.setObject(1, chatId);
            stmt.setObject(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                stmt.close();
                return count > 0;
            }
            stmt.close();
            return false;
        } catch (SQLException e) {
            log.error("Failed to check if user is participant", e);
            throw new RuntimeException("Failed to check participant", e);
        }
    }

    @Override
    public void deleteById(UUID chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(DELETE_SQL);
            stmt.setObject(1, chatId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("Failed to delete chat: {}", chatId, e);
            throw new RuntimeException("Failed to delete chat", e);
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
            log.error("Failed to count all chats", e);
            throw new RuntimeException("Failed to count chats", e);
        }
    }

    @Override
    public List<Chat> findAll() {
        try {
            PreparedStatement stmt = connection.prepareStatement(FIND_ALL_SQL);
            ResultSet rs = stmt.executeQuery();
            List<Chat> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRowToChat(rs));
            }
            stmt.close();
            return results;
        } catch (SQLException e) {
            log.error("Failed to find all chats", e);
            throw new RuntimeException("Failed to find all chats", e);
        }
    }

    private Chat mapRowToChat(ResultSet rs) throws SQLException {
        return new Chat(
            rs.getObject("chat_id", UUID.class),
            com.chatv2.common.model.ChatType.fromString(rs.getString("chat_type")),
            rs.getString("name"),
            rs.getString("description"),
            rs.getObject("owner_id", UUID.class),
            rs.getString("avatar_data"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            rs.getInt("participant_count")
        );
    }
}
