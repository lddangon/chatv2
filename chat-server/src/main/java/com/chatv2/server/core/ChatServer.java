package com.chatv2.server.core;

import com.chatv2.server.config.ServerProperties;
import com.chatv2.server.discovery.ServerDiscoveryBroadcaster;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main chat server class.
 */
public class ChatServer {
    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

    private final ServerConfig config;
    private final ChannelInitializer<SocketChannel> channelInitializer;
    private final ServerProperties serverProperties;
    private BootstrapFactory bootstrapFactory;
    private ServerDiscoveryBroadcaster broadcaster;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger connectedClients = new AtomicInteger(0);
    private final Map<UUID, Channel> clientChannels = new ConcurrentHashMap<>();
    private Instant startTime;

    /**
     * Creates a new chat server.
     */
    public ChatServer(ServerConfig config, ChannelInitializer<SocketChannel> channelInitializer,
                      ServerProperties serverProperties) {
        this.config = config;
        this.channelInitializer = channelInitializer;
        this.serverProperties = serverProperties;
    }

    /**
     * Creates a new chat server without server properties (for backward compatibility).
     */
    public ChatServer(ServerConfig config, ChannelInitializer<SocketChannel> channelInitializer) {
        this(config, channelInitializer, null);
    }

    /**
     * Starts the server.
     */
    public CompletableFuture<Void> start() {
        if (running.compareAndSet(false, true)) {
            log.info("Starting {} on {}:{}", config.getName(), config.getHost(), config.getPort());
            startTime = Instant.now();

            bootstrapFactory = new BootstrapFactory(config, channelInitializer);

            return bootstrapFactory.startServer()
                .thenAccept(channel -> {
                    log.info("Server started successfully");
                    // Start UDP broadcast for server discovery
                    if (config.isUdpEnabled()) {
                        startUdpBroadcast();
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to start server", ex);
                    running.set(false);
                    throw new RuntimeException("Failed to start server", ex);
                });
        } else {
            log.warn("Server is already running");
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Stops the server.
     */
    public CompletableFuture<Void> stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping server...");

            // Stop UDP broadcast
            stopUdpBroadcast();

            return bootstrapFactory.stopServer()
                .thenAccept(v -> {
                    log.info("Server stopped successfully");
                    clientChannels.clear();
                    connectedClients.set(0);
                })
                .exceptionally(ex -> {
                    log.error("Error stopping server", ex);
                    throw new RuntimeException("Failed to stop server", ex);
                });
        } else {
            log.warn("Server is not running");
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Gets the server state.
     */
    public ServerState getState() {
        if (!running.get()) {
            return ServerState.STOPPED;
        }
        if (bootstrapFactory != null && bootstrapFactory.isRunning()) {
            return ServerState.RUNNING;
        }
        return ServerState.STARTING;
    }

    /**
     * Gets the number of connected clients.
     */
    public int getConnectedClients() {
        return connectedClients.get();
    }

    /**
     * Gets the server uptime in seconds.
     */
    public long getUptimeSeconds() {
        if (startTime == null) {
            return 0;
        }
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }

    /**
     * Registers a client channel.
     */
    public void registerClient(UUID clientId, Channel channel) {
        clientChannels.put(clientId, channel);
        connectedClients.incrementAndGet();
        log.info("Client registered: {}. Total clients: {}", clientId, connectedClients.get());
        updateBroadcasterInfo();
    }

    /**
     * Unregisters a client channel.
     */
    public void unregisterClient(UUID clientId) {
        Channel channel = clientChannels.remove(clientId);
        if (channel != null) {
            connectedClients.decrementAndGet();
            log.info("Client unregistered: {}. Total clients: {}", clientId, connectedClients.get());
            updateBroadcasterInfo();
        }
    }

    /**
     * Gets a client channel by ID.
     */
    public Channel getClientChannel(UUID clientId) {
        return clientChannels.get(clientId);
    }

    /**
     * Gets all connected client IDs.
     */
    public Map<UUID, Channel> getAllClientChannels() {
        return Map.copyOf(clientChannels);
    }

    /**
     * Starts UDP broadcast for server discovery.
     */
    private void startUdpBroadcast() {
        try {
            if (serverProperties != null) {
                broadcaster = new ServerDiscoveryBroadcaster(serverProperties);
                broadcaster.start().thenRun(() -> {
                    log.info("UDP broadcast started successfully on {}:{}", 
                        config.getUdpMulticastAddress(), config.getUdpMulticastPort());
                    updateBroadcasterInfo();
                }).exceptionally(ex -> {
                    log.error("Failed to start UDP broadcast", ex);
                    return null;
                });
            } else {
                log.warn("ServerProperties is null, skipping UDP broadcast");
            }
        } catch (Exception e) {
            log.error("Error initializing UDP broadcast", e);
        }
    }

    /**
     * Stops UDP broadcast.
     */
    private void stopUdpBroadcast() {
        if (broadcaster != null && broadcaster.isRunning()) {
            broadcaster.stop().thenRun(() -> {
                log.info("UDP broadcast stopped successfully");
            }).exceptionally(ex -> {
                log.error("Failed to stop UDP broadcast", ex);
                return null;
            });
        }
    }

    /**
     * Updates broadcaster with current server information.
     */
    private void updateBroadcasterInfo() {
        if (broadcaster != null && broadcaster.isRunning()) {
            broadcaster.updateCurrentUsers(connectedClients.get());
            broadcaster.updateServerState(running.get() ? "ACTIVE" : "STOPPED");
        }
    }

    /**
     * Enum representing server state.
     */
    public enum ServerState {
        STOPPED,
        STARTING,
        RUNNING
    }
}
