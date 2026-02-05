package com.chatv2.client.core;

import com.chatv2.client.network.NetworkClient;
import com.chatv2.common.model.Message;
import com.chatv2.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Main chat client class.
 */
public class ChatClient {
    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    private final ClientConfig config;
    private final NetworkClient networkClient;
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private final ConcurrentHashMap<UUID, Consumer<Message>> messageConsumers = new ConcurrentHashMap<>();

    private UUID currentUserId;
    private String currentToken;
    private String connectedServerHost;
    private int connectedServerPort;

    public ChatClient(ClientConfig config) {
        this.config = config;
        this.networkClient = new NetworkClient();
    }

    /**
     * Connects to the server.
     */
    public CompletableFuture<Void> connect(String serverHost, int serverPort) {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.AUTHENTICATED) {
            log.warn("Already connected to server");
            return CompletableFuture.completedFuture(null);
        }

        setState(ConnectionState.CONNECTING);
        this.connectedServerHost = serverHost;
        this.connectedServerPort = serverPort;

        log.info("Connecting to server {}:{}", serverHost, serverPort);

        return networkClient.connect(serverHost, serverPort)
            .thenAccept(v -> {
                setState(ConnectionState.CONNECTED);
                log.info("Connected to server {}:{}", serverHost, serverPort);
            })
            .exceptionally(ex -> {
                setState(ConnectionState.ERROR);
                log.error("Failed to connect to server {}:{}", serverHost, serverPort, ex);
                throw new RuntimeException("Failed to connect to server", ex);
            });
    }

    /**
     * Disconnects from the server.
     */
    public CompletableFuture<Void> disconnect() {
        if (state == ConnectionState.DISCONNECTED) {
            return CompletableFuture.completedFuture(null);
        }

        log.info("Disconnecting from server");

        return networkClient.disconnect()
            .thenAccept(v -> {
                setState(ConnectionState.DISCONNECTED);
                currentUserId = null;
                currentToken = null;
                connectedServerHost = null;
                log.info("Disconnected from server");
            })
            .exceptionally(ex -> {
                setState(ConnectionState.ERROR);
                log.error("Failed to disconnect", ex);
                throw new RuntimeException("Failed to disconnect", ex);
            });
    }

    /**
     * Sends a message.
     */
    public CompletableFuture<Void> sendMessage(Message message) {
        if (state != ConnectionState.AUTHENTICATED) {
            log.warn("Cannot send message: not authenticated");
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }

        log.debug("Sending message: {}", message.messageId());

        String messageStr = String.format("MESSAGE_SEND:%s:%s:%s:%s",
            message.chatId(),
            message.senderId(),
            message.content(),
            message.messageType().name()
        );

        return networkClient.sendRequest(messageStr)
            .thenAccept(response -> {
                log.debug("Message sent successfully: {}", message.messageId());
            })
            .exceptionally(ex -> {
                log.error("Failed to send message: {}", message.messageId(), ex);
                throw new RuntimeException("Failed to send message", ex);
            });
    }

    /**
     * Authenticates with the server.
     */
    public CompletableFuture<Void> login(String username, String password) {
        if (state != ConnectionState.CONNECTED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to server"));
        }

        log.info("Authenticating user: {}", username);

        String loginRequest = String.format("AUTH_LOGIN:%s:%s", username, password);

        return networkClient.sendRequest(loginRequest)
            .thenAccept(response -> {
                if (response.contains("SUCCESS")) {
                    String[] parts = response.split(":");
                    if (parts.length >= 4) {
                        currentUserId = UUID.fromString(parts[2]);
                        currentToken = parts[3];
                        setState(ConnectionState.AUTHENTICATED);
                        log.info("Authentication successful for user: {}", username);
                    }
                } else {
                    setState(ConnectionState.CONNECTED);
                    log.warn("Authentication failed: {}", response);
                    throw new RuntimeException("Authentication failed");
                }
            })
            .exceptionally(ex -> {
                setState(ConnectionState.ERROR);
                log.error("Authentication error", ex);
                throw new RuntimeException("Authentication error", ex);
            });
    }

    /**
     * Registers a new user.
     */
    public CompletableFuture<Void> register(String username, String password, String fullName) {
        if (state != ConnectionState.CONNECTED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to server"));
        }

        log.info("Registering new user: {}", username);

        String registerRequest = String.format("AUTH_REGISTER:%s:%s:%s", username, password, fullName);

        return networkClient.sendRequest(registerRequest)
            .thenAccept(response -> {
                if (response.contains("SUCCESS")) {
                    log.info("Registration successful for user: {}", username);
                } else {
                    log.warn("Registration failed: {}", response);
                    throw new RuntimeException("Registration failed");
                }
            })
            .exceptionally(ex -> {
                log.error("Registration error", ex);
                throw new RuntimeException("Registration error", ex);
            });
    }

    /**
     * Gets the current user's profile.
     */
    public com.chatv2.common.model.UserProfile getProfile() throws Exception {
        if (state != ConnectionState.AUTHENTICATED) {
            throw new IllegalStateException("Not authenticated");
        }

        log.debug("Getting user profile for: {}", currentUserId);

        String profileRequest = String.format("USER_GET_PROFILE:%s", currentUserId);
        String response = networkClient.sendRequest(profileRequest).get();

        if (response.contains("SUCCESS")) {
            // Parse profile from response
            // Format: SUCCESS:username:fullName:bio:status
            String[] parts = response.split(":");
            if (parts.length >= 5) {
                String username = parts[1];
                String fullName = parts[2];
                String bio = parts[3];
                com.chatv2.common.model.UserStatus status = com.chatv2.common.model.UserStatus.valueOf(parts[4]);

                return new com.chatv2.common.model.UserProfile(
                    currentUserId,
                    username,
                    null, // passwordHash
                    null, // salt
                    fullName,
                    null, // avatarData
                    bio,
                    status,
                    null, // createdAt
                    null  // updatedAt
                );
            }
        }

        throw new RuntimeException("Failed to get profile");
    }

    /**
     * Updates the current user's profile.
     */
    public CompletableFuture<Void> updateProfile(String fullName, String bio, byte[] avatarData) {
        if (state != ConnectionState.AUTHENTICATED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated"));
        }

        log.debug("Updating profile for user: {}", currentUserId);

        String avatarBase64 = avatarData != null ? java.util.Base64.getEncoder().encodeToString(avatarData) : "";
        String updateRequest = String.format("USER_UPDATE_PROFILE:%s:%s:%s", fullName, bio, avatarBase64);

        return networkClient.sendRequest(updateRequest)
            .thenAccept(response -> {
                if (response.contains("SUCCESS")) {
                    log.info("Profile updated successfully");
                } else {
                    log.warn("Profile update failed: {}", response);
                    throw new RuntimeException("Profile update failed");
                }
            })
            .exceptionally(ex -> {
                log.error("Profile update error", ex);
                throw new RuntimeException("Profile update error", ex);
            });
    }

    /**
     * Sets a message consumer for incoming messages.
     */
    public void setMessageConsumer(Consumer<Message> consumer) {
        networkClient.setMessageHandler(message -> {
            if (message.contains("MESSAGE_RECEIVE")) {
                // Parse and deliver message
                try {
                    String[] parts = message.split(":");
                    if (parts.length >= 5) {
                        UUID chatId = UUID.fromString(parts[1]);
                        UUID senderId = UUID.fromString(parts[2]);
                        String content = parts[3];
                        String messageType = parts.length > 4 ? parts[4] : "TEXT";

                        Message msg = Message.createNew(chatId, senderId, content,
                            com.chatv2.common.model.MessageType.fromString(messageType));

                        // Notify consumers
                        messageConsumers.values().forEach(msgConsumer -> msgConsumer.accept(msg));
                    }
                } catch (Exception e) {
                    log.error("Failed to parse incoming message", e);
                }
            }
        });
    }

    /**
     * Registers a message consumer with unique ID.
     */
    public UUID registerMessageConsumer(Consumer<Message> consumer) {
        UUID consumerId = UUID.randomUUID();
        messageConsumers.put(consumerId, consumer);
        return consumerId;
    }

    /**
     * Unregisters a message consumer.
     */
    public void unregisterMessageConsumer(UUID consumerId) {
        messageConsumers.remove(consumerId);
    }

    /**
     * Gets the current connection state.
     */
    public ConnectionState getState() {
        return state;
    }

    /**
     * Gets the current user ID.
     */
    public UUID getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Gets the current session token.
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * Gets the connected server host.
     */
    public String getConnectedServerHost() {
        return connectedServerHost;
    }

    /**
     * Gets the connected server port.
     */
    public int getConnectedServerPort() {
        return connectedServerPort;
    }

    /**
     * Checks if client is authenticated.
     */
    public boolean isAuthenticated() {
        return state == ConnectionState.AUTHENTICATED;
    }

    /**
     * Sets the connection state.
     */
    private void setState(ConnectionState newState) {
        this.state = newState;
        log.debug("Connection state changed to: {}", newState.getDisplayName());
    }

    /**
     * Shuts down the client.
     */
    public CompletableFuture<Void> shutdown() {
        log.info("Shutting down client");
        messageConsumers.clear();
        return disconnect();
    }
}
