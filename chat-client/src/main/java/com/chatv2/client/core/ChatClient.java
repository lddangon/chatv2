package com.chatv2.client.core;

import com.chatv2.client.network.NetworkClient;
import com.chatv2.common.model.Message;
import com.chatv2.common.model.Session;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.MessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Main chat client class using binary protocol.
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

        // Initialize message handler for receiving server-initiated messages
        networkClient.setMessageHandler(message -> {
            if (message.getMessageType() == ProtocolMessageType.MESSAGE_RECEIVE) {
                try {
                    String jsonPayload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                    Message msg = MessageCodec.decode(jsonPayload, Message.class);

                    // Notify consumers
                    messageConsumers.values().forEach(msgConsumer -> msgConsumer.accept(msg));
                } catch (IOException e) {
                    log.error("Failed to parse incoming message", e);
                } catch (Exception e) {
                    log.error("Unexpected error processing incoming message", e);
                }
            }
        });

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

        try {
            byte[] payload = MessageCodec.encodeToBytes(message);
            ChatMessage request = new ChatMessage(
                ProtocolMessageType.MESSAGE_SEND_REQ,
                (byte) 0x00,
                message.messageId(),
                System.currentTimeMillis(),
                payload
            );

            return networkClient.sendRequest(request)
                .thenAccept(response -> {
                    log.debug("Message sent successfully: {}", message.messageId());
                })
                .exceptionally(ex -> {
                    log.error("Failed to send message: {}", message.messageId(), ex);
                    throw new RuntimeException("Failed to send message", ex);
                });
        } catch (Exception e) {
            log.error("Failed to encode message", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Authenticates with the server.
     */
    public CompletableFuture<Void> login(String username, String password) {
        if (state != ConnectionState.CONNECTED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to server"));
        }

        log.info("Authenticating user: {}", username);

        try {
            // Create login request as JSON
            Map<String, String> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("password", password);
            byte[] payload = MessageCodec.encodeToBytes(loginData);

            ChatMessage request = new ChatMessage(
                ProtocolMessageType.AUTH_LOGIN_REQ,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payload
            );

            return networkClient.sendRequest(request)
                .thenAccept(response -> {
                    if (response.getMessageType() == ProtocolMessageType.AUTH_LOGIN_RES) {
                        try {
                            // Deserialize Session from JSON payload
                            String jsonPayload = new String(response.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                            Session session = MessageCodec.decode(jsonPayload, Session.class);

                            currentUserId = session.userId();
                            currentToken = session.token();
                            setState(ConnectionState.AUTHENTICATED);
                            log.info("Authentication successful for user: {}", username);
                        } catch (IOException e) {
                            setState(ConnectionState.CONNECTED);
                            log.warn("Failed to decode authentication response", e);
                            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
                        }
                    } else {
                        setState(ConnectionState.CONNECTED);
                        log.warn("Authentication failed: unexpected response type {}", response.getMessageType());
                        throw new RuntimeException("Authentication failed");
                    }
                })
                .exceptionally(ex -> {
                    setState(ConnectionState.ERROR);
                    log.error("Authentication error", ex);
                    throw new RuntimeException("Authentication error", ex);
                });
        } catch (Exception e) {
            log.error("Failed to create login request", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Registers a new user.
     */
    public CompletableFuture<Void> register(String username, String password, String fullName) {
        if (state != ConnectionState.CONNECTED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to server"));
        }

        log.info("Registering new user: {}", username);

        try {
            // Create UserProfile for registration
            UserProfile userProfile = UserProfile.createNew(username, password, "salt", fullName, null);
            byte[] payload = MessageCodec.encodeToBytes(userProfile);

            ChatMessage request = new ChatMessage(
                ProtocolMessageType.AUTH_REGISTER_REQ,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payload
            );

            return networkClient.sendRequest(request)
                .thenAccept(response -> {
                    if (response.getMessageType() == ProtocolMessageType.AUTH_REGISTER_RES) {
                        log.info("Registration successful for user: {}", username);
                    } else {
                        log.warn("Registration failed: unexpected response type {}", response.getMessageType());
                        throw new RuntimeException("Registration failed");
                    }
                })
                .exceptionally(ex -> {
                    log.error("Registration error", ex);
                    throw new RuntimeException("Registration error", ex);
                });
        } catch (Exception e) {
            log.error("Failed to create registration request", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Gets the current user's profile.
     */
    public com.chatv2.common.model.UserProfile getProfile() throws Exception {
        if (state != ConnectionState.AUTHENTICATED) {
            throw new IllegalStateException("Not authenticated");
        }

        log.debug("Getting user profile for: {}", currentUserId);

        byte[] payload = MessageCodec.encodeToBytes(currentUserId.toString());
        ChatMessage request = new ChatMessage(
            ProtocolMessageType.USER_GET_PROFILE_REQ,
            (byte) 0x00,
            UUID.randomUUID(),
            System.currentTimeMillis(),
            payload
        );

        ChatMessage response = networkClient.sendRequest(request).get();

        if (response.getMessageType() == ProtocolMessageType.USER_GET_PROFILE_RES) {
            try {
                String jsonPayload = new String(response.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                return MessageCodec.decode(jsonPayload, com.chatv2.common.model.UserProfile.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to decode profile response", e);
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

        try {
            // Create profile update request
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("userId", currentUserId);
            updateData.put("fullName", fullName);
            updateData.put("bio", bio);
            if (avatarData != null) {
                updateData.put("avatarData", avatarData);
            }
            byte[] payload = MessageCodec.encodeToBytes(updateData);

            ChatMessage request = new ChatMessage(
                ProtocolMessageType.USER_UPDATE_PROFILE_REQ,
                (byte) 0x00,
                UUID.randomUUID(),
                System.currentTimeMillis(),
                payload
            );

            return networkClient.sendRequest(request)
                .thenAccept(response -> {
                    if (response.getMessageType() == ProtocolMessageType.USER_UPDATE_PROFILE_RES) {
                        log.info("Profile updated successfully");
                    } else {
                        log.warn("Profile update failed: unexpected response type {}", response.getMessageType());
                        throw new RuntimeException("Profile update failed");
                    }
                })
                .exceptionally(ex -> {
                    log.error("Profile update error", ex);
                    throw new RuntimeException("Profile update error", ex);
                });
        } catch (Exception e) {
            log.error("Failed to create profile update request", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sets a message consumer for incoming messages.
     */
    public void setMessageConsumer(Consumer<Message> consumer) {
        networkClient.setMessageHandler(message -> {
            if (message.getMessageType() == ProtocolMessageType.MESSAGE_RECEIVE) {
                try {
                    String jsonPayload = new String(message.getPayload(), java.nio.charset.StandardCharsets.UTF_8);
                    Message msg = MessageCodec.decode(jsonPayload, Message.class);

                    // Notify consumers
                    messageConsumers.values().forEach(msgConsumer -> msgConsumer.accept(msg));
                } catch (IOException e) {
                    log.error("Failed to parse incoming message", e);
                } catch (Exception e) {
                    log.error("Unexpected error processing incoming message", e);
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
