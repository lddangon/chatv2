package com.chatv2.server.discovery;

import com.chatv2.server.config.ServerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UDP multicast broadcaster for server discovery.
 * Periodically sends server information to multicast address for client auto-discovery.
 */
public class ServerDiscoveryBroadcaster implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ServerDiscoveryBroadcaster.class);
    
    private final ServerProperties properties;
    private final ObjectMapper objectMapper;
    private final NioEventLoopGroup eventLoopGroup;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;
    private final int broadcastInterval;
    private final String multicastAddress;
    private final int multicastPort;
    private final String serverAddress;
    private final int serverPort;
    
    private NioDatagramChannel datagramChannel;
    private volatile int currentUsers = 0;
    private volatile String serverState = "ACTIVE";
    
    /**
     * Creates a new server discovery broadcaster.
     *
     * @param properties server properties containing UDP configuration
     */
    public ServerDiscoveryBroadcaster(ServerProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("ServerProperties cannot be null");
        }
        
        this.properties = properties;
        this.multicastAddress = properties.getUdpConfig().getMulticastAddress();
        this.multicastPort = properties.getUdpConfig().getPort();
        this.broadcastInterval = properties.getUdpConfig().getBroadcastInterval();
        this.serverAddress = properties.getHost();
        this.serverPort = properties.getPort();
        this.running = new AtomicBoolean(false);
        this.eventLoopGroup = new NioEventLoopGroup();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        
        // Configure ObjectMapper for JSON serialization
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        log.info("ServerDiscoveryBroadcaster initialized: {}:{} every {} seconds",
            multicastAddress, multicastPort, broadcastInterval);
    }
    
    /**
     * Starts the UDP multicast broadcaster.
     *
     * @return CompletableFuture that completes when broadcaster is started
     */
    public CompletableFuture<Void> start() {
        if (running.compareAndSet(false, true)) {
            return CompletableFuture.runAsync(() -> {
                try {
                    log.info("Starting UDP multicast broadcaster on {}:{}", multicastAddress, multicastPort);

                    // Create UDP channel and register with event loop
                    datagramChannel = new NioDatagramChannel(InternetProtocolFamily.IPv4);
                    eventLoopGroup.register(datagramChannel).sync();
                    datagramChannel.config().setOption(io.netty.channel.ChannelOption.SO_BROADCAST, true);
                    datagramChannel.config().setOption(io.netty.channel.ChannelOption.SO_REUSEADDR, true);
                    datagramChannel.bind(new InetSocketAddress(0)).sync();
                    
                    log.info("UDP channel bound to local address: {}", datagramChannel.localAddress());
                    
                    // Schedule periodic broadcasts
                    scheduler.scheduleAtFixedRate(
                        this,
                        0,
                        broadcastInterval,
                        TimeUnit.SECONDS
                    );
                    
                    log.info("UDP multicast broadcaster started successfully");
                } catch (Exception e) {
                    log.error("Failed to start UDP multicast broadcaster", e);
                    running.set(false);
                    throw new RuntimeException("Failed to start broadcaster", e);
                }
            }, eventLoopGroup.next());
        } else {
            log.warn("UDP multicast broadcaster is already running");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Stops the UDP multicast broadcaster.
     *
     * @return CompletableFuture that completes when broadcaster is stopped
     */
    public CompletableFuture<Void> stop() {
        if (running.compareAndSet(true, false)) {
            return CompletableFuture.runAsync(() -> {
                try {
                    log.info("Stopping UDP multicast broadcaster");
                    
                    // Shutdown scheduler
                    scheduler.shutdown();
                    if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                    
                    // Close channel
                    if (datagramChannel != null && datagramChannel.isOpen()) {
                        datagramChannel.close().sync();
                    }
                    
                    // Shutdown event loop group
                    eventLoopGroup.shutdownGracefully();
                    
                    log.info("UDP multicast broadcaster stopped successfully");
                } catch (Exception e) {
                    log.error("Error stopping UDP multicast broadcaster", e);
                    throw new RuntimeException("Failed to stop broadcaster", e);
                }
            });
        } else {
            log.warn("UDP multicast broadcaster is not running");
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * Runs the broadcast task. Sends server information via UDP multicast.
     */
    @Override
    public void run() {
        if (!running.get()) {
            return;
        }
        
        try {
            // Create discovery packet
            DiscoveryPacket packet = createDiscoveryPacket();
            
            // Serialize to JSON
            String json = objectMapper.writeValueAsString(packet);
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            
            // Create and send datagram packet
            InetSocketAddress targetAddress = new InetSocketAddress(multicastAddress, multicastPort);
            DatagramPacket datagramPacket = new DatagramPacket(
                io.netty.buffer.Unpooled.wrappedBuffer(payload),
                targetAddress
            );
            
            datagramChannel.writeAndFlush(datagramPacket).await();
            
            log.debug("Broadcast sent to {}:{}", multicastAddress, multicastPort);
            log.trace("Broadcast payload: {}", json);
            
        } catch (Exception e) {
            log.error("Failed to send broadcast", e);
        }
    }
    
    /**
     * Creates a discovery packet with current server information.
     *
     * @return DiscoveryPacket containing server information
     */
    private DiscoveryPacket createDiscoveryPacket() {
        ServerProperties.EncryptionConfig encryptionConfig = properties.getEncryptionConfig();
        
        return new DiscoveryPacket(
            UUID.randomUUID().toString(),
            properties.getServerName(),
            serverAddress.equals("0.0.0.0") ? "127.0.0.1" : serverAddress,
            serverPort,
            "1.0.0",
            1000,
            currentUsers,
            encryptionConfig.isRequired(),
            encryptionConfig.isRequired() ? encryptionConfig.getDefaultPlugin() : null,
            serverState
        );
    }
    
    /**
     * Updates the current number of connected users.
     * This information is included in broadcast packets.
     *
     * @param currentUsers the current number of connected users
     */
    public void updateCurrentUsers(int currentUsers) {
        this.currentUsers = Math.max(0, currentUsers);
        log.debug("Updated current users: {}", this.currentUsers);
    }
    
    /**
     * Updates the server state.
     *
     * @param state the current server state (e.g., "ACTIVE", "MAINTENANCE")
     */
    public void updateServerState(String state) {
        if (state != null && !state.isBlank()) {
            this.serverState = state;
            log.debug("Updated server state: {}", this.serverState);
        }
    }
    
    /**
     * Checks if the broadcaster is currently running.
     *
     * @return true if broadcaster is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Gets the multicast address being used.
     *
     * @return the multicast address
     */
    public String getMulticastAddress() {
        return multicastAddress;
    }
    
    /**
     * Gets the multicast port being used.
     *
     * @return the multicast port
     */
    public int getMulticastPort() {
        return multicastPort;
    }
    
    /**
     * Gets the broadcast interval in seconds.
     *
     * @return the broadcast interval
     */
    public int getBroadcastInterval() {
        return broadcastInterval;
    }
    
    /**
     * Discovery packet record for UDP multicast.
     * Contains server information for client discovery.
     */
    public record DiscoveryPacket(
        String serverId,
        String serverName,
        String address,
        int port,
        String version,
        int maxUsers,
        int currentUsers,
        boolean encryptionRequired,
        String encryptionType,
        String state
    ) {
        /**
         * Creates a discovery packet with validation.
         */
        public DiscoveryPacket {
            if (serverId == null || serverId.isBlank()) {
                serverId = UUID.randomUUID().toString();
            }
            if (serverName == null || serverName.isBlank()) {
                throw new IllegalArgumentException("Server name cannot be null or blank");
            }
            if (address == null || address.isBlank()) {
                throw new IllegalArgumentException("Address cannot be null or blank");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            if (version == null || version.isBlank()) {
                version = "1.0.0";
            }
            if (maxUsers < 1) {
                maxUsers = 1000;
            }
            if (currentUsers < 0) {
                currentUsers = 0;
            }
            if (state == null || state.isBlank()) {
                state = "ACTIVE";
            }
        }
        
        /**
         * Checks if the server is full.
         *
         * @return true if server is at capacity
         */
        public boolean isFull() {
            return currentUsers >= maxUsers;
        }
        
        /**
         * Calculates server load percentage.
         *
         * @return load as percentage (0.0 to 100.0)
         */
        public double getLoadPercentage() {
            return (double) currentUsers / maxUsers * 100.0;
        }
        
        /**
         * Gets the display string for the server.
         *
         * @return formatted display string
         */
        public String getDisplayString() {
            return String.format("%s (%s:%d) - %d/%d users [%s]",
                serverName, address, port, currentUsers, maxUsers, state);
        }
    }
}
