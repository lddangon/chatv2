package com.chatv2.server.storage;

import com.chatv2.common.model.Chat;
import com.chatv2.common.model.Message;
import com.chatv2.common.model.Session;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Database manager for H2 embedded database.
 */
public class DatabaseManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final Connection connection;
    private final String databasePath;

    public DatabaseManager(String databasePath) {
        this.databasePath = databasePath;
        try {
            String url = "jdbc:h2:" + databasePath + ";MODE=PostgreSQL;AUTO_SERVER=TRUE";
            this.connection = DriverManager.getConnection(url, "sa", "");
            initializeSchema();
            log.info("Database initialized: {}", databasePath);
        } catch (SQLException e) {
            log.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Initializes database schema.
     */
    private void initializeSchema() throws SQLException {
        Statement stmt = connection.createStatement();

        // Users table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                user_id UUID PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                salt VARCHAR(64) NOT NULL,
                full_name VARCHAR(100),
                avatar_data VARCHAR(10000),
                bio VARCHAR(500),
                status VARCHAR(20) DEFAULT 'OFFLINE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // Sessions table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS sessions (
                session_id UUID PRIMARY KEY,
                user_id UUID NOT NULL,
                token VARCHAR(255) UNIQUE NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                device_info VARCHAR(255),
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
        """);

        // Chats table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS chats (
                chat_id UUID PRIMARY KEY,
                chat_type VARCHAR(20) NOT NULL,
                name VARCHAR(100),
                description VARCHAR(500),
                owner_id UUID,
                avatar_data VARCHAR(10000),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                participant_count INT DEFAULT 0,
                FOREIGN KEY (owner_id) REFERENCES users(user_id)
            )
        """);

        // Chat participants table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS chat_participants (
                id UUID PRIMARY KEY,
                chat_id UUID NOT NULL,
                user_id UUID NOT NULL,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                role VARCHAR(20) DEFAULT 'MEMBER',
                UNIQUE (chat_id, user_id),
                FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
        """);

        // Messages table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS messages (
                message_id UUID PRIMARY KEY,
                chat_id UUID NOT NULL,
                sender_id UUID NOT NULL,
                content TEXT NOT NULL,
                message_type VARCHAR(20) DEFAULT 'TEXT',
                reply_to_message_id UUID,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                edited_at TIMESTAMP,
                deleted_at TIMESTAMP,
                FOREIGN KEY (chat_id) REFERENCES chats(chat_id) ON DELETE CASCADE,
                FOREIGN KEY (sender_id) REFERENCES users(user_id),
                FOREIGN KEY (reply_to_message_id) REFERENCES messages(message_id)
            )
        """);

        // Message read receipts table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS message_read_receipts (
                receipt_id UUID PRIMARY KEY,
                message_id UUID NOT NULL,
                user_id UUID NOT NULL,
                read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (message_id, user_id),
                FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
        """);

        // Create indexes
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_username ON users(username)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_session_token ON sessions(token)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_session_user_id ON sessions(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_type ON chats(chat_type)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_chat ON messages(chat_id, created_at)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id)");

        stmt.close();
        log.info("Database schema initialized successfully");
    }

    /**
     * Gets the database connection.
     */
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("Database connection closed");
            }
        } catch (SQLException e) {
            log.error("Failed to close database connection", e);
        }
    }

    /**
     * Creates repository instances.
     */
    public UserRepository createUserRepository() {
        return new UserRepositoryImpl(connection);
    }

    public ChatRepository createChatRepository() {
        return new ChatRepositoryImpl(connection);
    }

    public SessionRepository createSessionRepository() {
        return new SessionRepositoryImpl(connection);
    }

    public MessageRepository createMessageRepository() {
        return new MessageRepositoryImpl(connection);
    }

    /**
     * Gets the user repository instance.
     */
    public UserRepository getUserRepository() {
        return createUserRepository();
    }

    /**
     * Gets the chat repository instance.
     */
    public ChatRepository getChatRepository() {
        return createChatRepository();
    }

    /**
     * Gets the message repository instance.
     */
    public MessageRepository getMessageRepository() {
        return createMessageRepository();
    }

    /**
     * Gets the session repository instance.
     */
    public SessionRepository getSessionRepository() {
        return createSessionRepository();
    }

    // Manager instances (lazy initialization)
    private com.chatv2.server.manager.UserManager userManager;
    private com.chatv2.server.manager.ChatManager chatManager;
    private com.chatv2.server.manager.MessageManager messageManager;

    /**
     * Gets the user manager instance.
     */
    public com.chatv2.server.manager.UserManager getUserManager() {
        if (userManager == null) {
            userManager = new com.chatv2.server.manager.UserManager(createUserRepository());
        }
        return userManager;
    }

    /**
     * Gets the chat manager instance.
     */
    public com.chatv2.server.manager.ChatManager getChatManager() {
        if (chatManager == null) {
            chatManager = new com.chatv2.server.manager.ChatManager(createChatRepository());
        }
        return chatManager;
    }

    /**
     * Gets the message manager instance.
     */
    public com.chatv2.server.manager.MessageManager getMessageManager() {
        if (messageManager == null) {
            messageManager = new com.chatv2.server.manager.MessageManager(createMessageRepository());
        }
        return messageManager;
    }
}
