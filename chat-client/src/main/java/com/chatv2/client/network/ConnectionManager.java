package com.chatv2.client.network;

import com.chatv2.client.config.ClientProperties;
import com.chatv2.client.core.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Connection manager for managing client-server connection lifecycle.
 * Handles automatic reconnection with exponential backoff strategy.
 */
public class ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    
    private final NetworkClient networkClient;
    private final ClientProperties.ConnectionConfig config;
    private final List<ConnectionStateListener> listeners;
    private final ReentrantLock stateLock;
    
    private volatile ConnectionState state;
    private volatile String currentHost;
    private volatile int currentPort;
    
    private ScheduledExecutorService reconnectScheduler;
    private final AtomicInteger reconnectAttempts;
    private final AtomicBoolean manualDisconnect;
    private final int maxReconnectAttempts;
    private final int baseReconnectDelay;
    
    /**
     * Interface for connection state change notifications.
     */
    public interface ConnectionStateListener {
        /**
         * Called when connection state changes.
         *
         * @param oldState previous connection state
         * @param newState new connection state
         */
        void onStateChanged(ConnectionState oldState, ConnectionState newState);
        
        /**
         * Called when attempting to reconnect.
         *
         * @param attempt current attempt number
         * @param maxAttempts maximum number of attempts
         */
        default void onReconnecting(int attempt, int maxAttempts) {
        }
        
        /**
         * Called when a connection error occurs.
         *
         * @param error exception that caused error
         */
        default void onConnectionError(Exception error) {
        }
    }
    
    /**
     * Creates a new connection manager.
     *
     * @param networkClient underlying network client
     * @param config connection configuration
     */
    public ConnectionManager(NetworkClient networkClient, ClientProperties.ConnectionConfig config) {
        if (networkClient == null) {
            throw new IllegalArgumentException("NetworkClient cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("ConnectionConfig cannot be null");
        }
        
        this.networkClient = networkClient;
        this.config = config;
        this.state = ConnectionState.DISCONNECTED;
        this.listeners = new CopyOnWriteArrayList<>();
        this.stateLock = new ReentrantLock();
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("reconnect-scheduler").factory()
        );
        this.reconnectAttempts = new AtomicInteger(0);
        this.manualDisconnect = new AtomicBoolean(false);
        this.maxReconnectAttempts = config.reconnectAttempts();
        this.baseReconnectDelay = config.reconnectDelaySeconds();
        
        log.info("ConnectionManager initialized with maxAttempts={}, baseDelay={}s",
            maxReconnectAttempts, baseReconnectDelay);
    }
    
    /**
     * Connects to specified server.
     *
     * @param host server host
     * @param port server port
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Void> connect(String host, int port) {
        if (host == null || host.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Host cannot be null or blank"));
        }
        if (port < 1 || port > 65535) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Port must be between 1 and 65535"));
        }
        
        stateLock.lock();
        try {
            if (state.isConnected()) {
                log.warn("Already connected to {}:{}. Ignoring new connection request.", currentHost, currentPort);
                return CompletableFuture.completedFuture(null);
            }
            
            // Transition to CONNECTING state
            ConnectionState oldState = state;
            updateState(ConnectionState.CONNECTING);
            
            // Store connection parameters
            this.currentHost = host;
            this.currentPort = port;
            
            // Reset manual disconnect flag and attempt counter
            manualDisconnect.set(false);
            reconnectAttempts.set(0);
            
            log.info("Connecting to {}:{}", host, port);
            
        } finally {
            stateLock.unlock();
        }
        
        // Attempt connection
        return networkClient.connect(host, port)
            .thenRun(() -> {
                stateLock.lock();
                try {
                    updateState(ConnectionState.CONNECTED);
                    reconnectAttempts.set(0);
                    log.info("Successfully connected to {}:{}", host, port);
                } finally {
                    stateLock.unlock();
                }
            })
            .whenComplete((result, error) -> {
                if (error != null) {
                    stateLock.lock();
                    try {
                        updateState(ConnectionState.ERROR);
                        log.error("Failed to connect to {}:{}", host, port, error);
                        
                        // Start reconnection process if not manual disconnect
                        if (!manualDisconnect.get()) {
                            scheduleReconnect(host, port);
                        }
                    } finally {
                        stateLock.unlock();
                    }
                    notifyConnectionError((Exception) error);
                }
            });
    }
    
    /**
     * Disconnects from server.
     * Cancels any pending reconnection attempts.
     *
     * @return CompletableFuture that completes when disconnected
     */
    public CompletableFuture<Void> disconnect() {
        stateLock.lock();
        try {
            ConnectionState oldState = state;
            
            // Set manual disconnect flag to prevent auto-reconnect
            manualDisconnect.set(true);
            
            // Update state
            updateState(ConnectionState.DISCONNECTED);
            
            log.info("Disconnecting from {}:{}", currentHost, currentPort);
            
        } finally {
            stateLock.unlock();
        }
        
        // Cancel scheduled reconnect tasks - using shutdown since getQueue() is not available in ScheduledExecutorService
        reconnectScheduler.shutdownNow();
        // Create new scheduler after shutdown
        this.reconnectScheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("reconnect-scheduler").factory()
        );
        
        // Disconnect network client
        return networkClient.disconnect()
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Error during disconnect", error);
                }
                log.info("Disconnected successfully");
            });
    }
    
    /**
     * Schedules a reconnection attempt with exponential backoff.
     *
     * @param host the server host
     * @param port the server port
     */
    private void scheduleReconnect(String host, int port) {
        stateLock.lock();
        try {
            int attempt = reconnectAttempts.incrementAndGet();
            
            // Check if we've exceeded max attempts
            if (attempt > maxReconnectAttempts) {
                log.warn("Max reconnection attempts ({}) exceeded. Giving up.", maxReconnectAttempts);
                updateState(ConnectionState.ERROR);
                return;
            }
            
            // Calculate delay with exponential backoff
            // Attempt 1: 0s (immediate), 2: 5s, 3: 10s, 4: 20s, 5: 40s
            long delaySeconds = switch (attempt) {
                case 1 -> 0;
                case 2 -> baseReconnectDelay;
                default -> (long) baseReconnectDelay * (1L << (attempt - 2));
            };
            
            // Cap maximum delay at 5 minutes
            delaySeconds = Math.min(delaySeconds, 300);
            
            updateState(ConnectionState.RECONNECTING);
            
            log.info("Scheduling reconnect attempt {}/{} to {}:{} in {}s",
                attempt, maxReconnectAttempts, host, port, delaySeconds);
            
            notifyReconnecting(attempt, maxReconnectAttempts);
            
            // Schedule reconnection attempt
            reconnectScheduler.schedule(() -> attemptReconnect(host, port), delaySeconds, TimeUnit.SECONDS);
            
        } finally {
            stateLock.unlock();
        }
    }
    
    /**
     * Attempts to reconnect to server.
     *
     * @param host the server host
     * @param port the server port
     */
    private void attemptReconnect(String host, int port) {
        if (!isRunning()) {
            log.debug("Reconnection cancelled - manager not running");
            return;
        }
        
        stateLock.lock();
        try {
            // Update state to CONNECTING
            updateState(ConnectionState.CONNECTING);
            
        } finally {
            stateLock.unlock();
        }
        
        log.info("Attempting to reconnect to {}:{}", host, port);
        
        networkClient.connect(host, port)
            .thenRun(() -> {
                stateLock.lock();
                try {
                    updateState(ConnectionState.CONNECTED);
                    reconnectAttempts.set(0);
                    log.info("Reconnection successful to {}:{}", host, port);
                } finally {
                    stateLock.unlock();
                }
            })
            .whenComplete((result, error) -> {
                if (error != null) {
                    stateLock.lock();
                    try {
                        updateState(ConnectionState.ERROR);
                        log.error("Reconnection attempt failed to {}:{}", host, port, error);
                        
                        // Schedule next attempt if not manual disconnect
                        if (!manualDisconnect.get()) {
                            scheduleReconnect(host, port);
                        }
                    } finally {
                        stateLock.unlock();
                    }
                    notifyConnectionError((Exception) error);
                }
            });
    }
    
    /**
     * Checks if connection is currently active.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return state.isConnected();
    }
    
    /**
     * Checks if manager is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return !state.equals(ConnectionState.DISCONNECTED) || manualDisconnect.get();
    }
    
    /**
     * Gets the current connection state.
     *
     * @return current ConnectionState
     */
    public ConnectionState getState() {
        return state;
    }
    
    /**
     * Gets the current host.
     *
     * @return the host address
     */
    public String getCurrentHost() {
        return currentHost;
    }
    
    /**
     * Gets the current port.
     *
     * @return the port number
     */
    public int getCurrentPort() {
        return currentPort;
    }
    
    /**
     * Gets the number of reconnection attempts.
     *
     * @return the number of attempts
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }
    
    /**
     * Gets the network client.
     *
     * @return the NetworkClient instance
     */
    public NetworkClient getNetworkClient() {
        return networkClient;
    }
    
    /**
     * Adds a connection state listener.
     *
     * @param listener the listener to add
     */
    public void addListener(ConnectionStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
            log.debug("Added connection state listener: {}", listener.getClass().getSimpleName());
        }
    }
    
    /**
     * Removes a connection state listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(ConnectionStateListener listener) {
        listeners.remove(listener);
        log.debug("Removed connection state listener");
    }
    
    /**
     * Shuts down the connection manager gracefully.
     * Cancels all pending operations and closes connections.
     *
     * @return CompletableFuture that completes when shutdown is complete
     */
    public CompletableFuture<Void> shutdown() {
        log.info("Shutting down ConnectionManager");
        
        stateLock.lock();
        try {
            manualDisconnect.set(true);
            updateState(ConnectionState.DISCONNECTED);
        } finally {
            stateLock.unlock();
        }
        
        // Shutdown scheduler
        reconnectScheduler.shutdown();
        CompletableFuture<Void> schedulerFuture = CompletableFuture.runAsync(() -> {
            try {
                if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    reconnectScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Disconnect network client
        return CompletableFuture.allOf(schedulerFuture, networkClient.shutdown())
            .whenComplete((result, error) -> {
                listeners.clear();
                log.info("ConnectionManager shutdown complete");
            });
    }
    
    /**
     * Updates the connection state and notifies listeners.
     *
     * @param newState the new state
     */
    private void updateState(ConnectionState newState) {
        ConnectionState oldState = this.state;
        this.state = newState;
        notifyStateChanged(oldState, newState);
    }
    
    /**
     * Notifies all listeners of a state change.
     *
     * @param oldState the previous state
     * @param newState the new state
     */
    private void notifyStateChanged(ConnectionState oldState, ConnectionState newState) {
        if (oldState != newState) {
            log.debug("Connection state changed: {} -> {}", oldState, newState);
            listeners.forEach(listener -> {
                try {
                    listener.onStateChanged(oldState, newState);
                } catch (Exception e) {
                    log.error("Error notifying state change listener", e);
                }
            });
        }
    }
    
    /**
     * Notifies all listeners of a reconnection attempt.
     *
     * @param attempt the current attempt number
     * @param maxAttempts the maximum number of attempts
     */
    private void notifyReconnecting(int attempt, int maxAttempts) {
        listeners.forEach(listener -> {
            try {
                listener.onReconnecting(attempt, maxAttempts);
            } catch (Exception e) {
                log.error("Error notifying reconnect listener", e);
            }
        });
    }
    
    /**
     * Notifies all listeners of a connection error.
     *
     * @param error the exception
     */
    private void notifyConnectionError(Exception error) {
        listeners.forEach(listener -> {
            try {
                listener.onConnectionError(error);
            } catch (Exception e) {
                log.error("Error notifying error listener", e);
            }
        });
    }
    
    // Flag for checking if manager is still running
    private final AtomicBoolean running = new AtomicBoolean(true);
}