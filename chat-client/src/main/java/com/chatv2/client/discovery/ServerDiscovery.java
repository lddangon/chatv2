package com.chatv2.client.discovery;

import com.chatv2.common.crypto.CryptoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Server discovery via UDP multicast.
 */
public class ServerDiscovery {
    private static final Logger log = LoggerFactory.getLogger(ServerDiscovery.class);

    private final String multicastAddress;
    private final int multicastPort;
    private final int timeoutSeconds;

    private DatagramSocket socket;
    private MulticastSocket multicastSocket;
    private boolean running = false;
    private final ConcurrentHashMap<String, ServerInfo> discoveredServers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final List<Consumer<ServerInfo>> listeners = new ArrayList<>();

    public ServerDiscovery(String multicastAddress, int multicastPort, int timeoutSeconds) {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Starts server discovery.
     */
    public CompletableFuture<Void> startDiscovery() {
        if (running) {
            log.warn("Discovery already running");
            return CompletableFuture.completedFuture(null);
        }

        running = true;
        discoveredServers.clear();

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting server discovery on {}:{}", multicastAddress, multicastPort);

                // Create multicast socket
                multicastSocket = new MulticastSocket(multicastPort);
                multicastSocket.setTimeToLive(4);
                multicastSocket.joinGroup(InetAddress.getByName(multicastAddress));

                // Send discovery request
                sendDiscoveryRequest();

                // Start listener
                startListener();

                log.info("Server discovery started");
                return null;
            } catch (Exception e) {
                log.error("Failed to start discovery", e);
                running = false;
                throw new RuntimeException("Failed to start discovery", e);
            }
        }, executor);
    }

    /**
     * Stops server discovery.
     */
    public CompletableFuture<Void> stopDiscovery() {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }

        running = false;

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Stopping server discovery");

                if (multicastSocket != null && !multicastSocket.isClosed()) {
                    multicastSocket.leaveGroup(InetAddress.getByName(multicastAddress));
                    multicastSocket.close();
                }

                log.info("Server discovery stopped");
                return null;
            } catch (Exception e) {
                log.error("Failed to stop discovery", e);
                throw new RuntimeException("Failed to stop discovery", e);
            }
        }, executor);
    }

    /**
     * Gets all discovered servers.
     */
    public List<ServerInfo> getDiscoveredServers() {
        return new ArrayList<>(discoveredServers.values());
    }

    /**
     * Registers a listener for server discovery events.
     */
    public void addListener(Consumer<ServerInfo> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     */
    public void removeListener(Consumer<ServerInfo> listener) {
        listeners.remove(listener);
    }

    /**
     * Sends a discovery request.
     */
    private void sendDiscoveryRequest() {
        try {
            String clientId = CryptoUtils.generateUuid();
            String request = String.format(
                "SERVICE_DISCOVERY_REQ:%s:%s:%s:%s",
                clientId,
                "1.0.0",
                "Windows",
                true
            );

            byte[] data = request.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(multicastAddress);

            DatagramPacket packet = new DatagramPacket(data, data.length, address, multicastPort);
            multicastSocket.send(packet);

            log.debug("Sent discovery request");
        } catch (Exception e) {
            log.error("Failed to send discovery request", e);
        }
    }

    /**
     * Starts the listener for server responses.
     */
    private void startListener() {
        CompletableFuture.runAsync(() -> {
            byte[] buffer = new byte[1024];

            while (running && !multicastSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);

                    String response = new String(packet.getData(), 0, packet.getLength(),
                        java.nio.charset.StandardCharsets.UTF_8);

                    if (response.contains("SERVICE_DISCOVERY_RES")) {
                        parseDiscoveryResponse(response, packet.getAddress().getHostAddress());
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("Error in discovery listener", e);
                    }
                }
            }
        }, executor);
    }

    /**
     * Parses a discovery response.
     */
    private void parseDiscoveryResponse(String response, String senderAddress) {
        try {
            String[] parts = response.split(":");
            if (parts.length >= 10) {
                ServerInfo serverInfo = new ServerInfo(
                    parts[1], // serverId
                    parts[2], // serverName
                    senderAddress,
                    Integer.parseInt(parts[4]), // port
                    parts[3], // version
                    Boolean.parseBoolean(parts[5]), // encryptionRequired
                    parts[6], // encryptionType
                    Integer.parseInt(parts[7]), // currentUsers
                    Integer.parseInt(parts[8]), // maxUsers
                    Instant.now()
                );

                // Store server info
                discoveredServers.put(serverInfo.serverId(), serverInfo);

                log.info("Discovered server: {} at {}:{}", serverInfo.serverName(),
                    serverInfo.address(), serverInfo.port());

                // Notify listeners
                listeners.forEach(listener -> listener.accept(serverInfo));
            }
        } catch (Exception e) {
            log.error("Failed to parse discovery response: {}", response, e);
        }
    }

    /**
     * Shuts down the discovery service.
     */
    public CompletableFuture<Void> shutdown() {
        log.info("Shutting down server discovery");
        listeners.clear();
        discoveredServers.clear();
        return stopDiscovery();
    }
}
