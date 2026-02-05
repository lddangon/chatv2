package com.chatv2.client.network;

import com.chatv2.client.config.ClientProperties;
import com.chatv2.client.core.ConnectionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConnectionManager class.
 */
@ExtendWith(MockitoExtension.class)
public class ConnectionManagerTest {

    @Mock
    private NetworkClient mockNetworkClient;

    private ClientProperties.ConnectionConfig config;
    private ConnectionManager connectionManager;

    @BeforeEach
    public void setUp() {
        // Create a simple config for testing
        config = new ClientProperties.ConnectionConfig(
            5,  // reconnectAttempts
            5,  // reconnectDelaySeconds
            30  // heartbeatIntervalSeconds
        );
        
        connectionManager = new ConnectionManager(mockNetworkClient, config);
    }

    @Test
    public void testInitialization() {
        assertNotNull(connectionManager);
        assertEquals(ConnectionState.DISCONNECTED, connectionManager.getState());
        assertFalse(connectionManager.isConnected());
        assertEquals(0, connectionManager.getReconnectAttempts());
        assertEquals(mockNetworkClient, connectionManager.getNetworkClient());
    }

    @Test
    public void testConnectSuccessful() {
        // Setup
        String host = "localhost";
        int port = 8080;
        
        when(mockNetworkClient.connect(anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // Call the method
        CompletableFuture<Void> future = connectionManager.connect(host, port);
        
        // Verify
        assertTrue(future.isDone(), "Future should be completed");
        assertEquals(host, connectionManager.getCurrentHost());
        assertEquals(port, connectionManager.getCurrentPort());
        assertEquals(ConnectionState.CONNECTED, connectionManager.getState());
        verify(mockNetworkClient).connect(host, port);
    }

    @Test
    public void testConnectFailure() {
        // Setup
        String host = "localhost";
        int port = 8080;
        RuntimeException exception = new RuntimeException("Connection failed");
        
        when(mockNetworkClient.connect(anyString(), anyInt()))
            .thenReturn(CompletableFuture.failedFuture(exception));
        
        // Add a listener to capture the error
        ConnectionManager.ConnectionStateListener listener = mock(ConnectionManager.ConnectionStateListener.class);
        connectionManager.addListener(listener);
        
        // Call the method
        CompletableFuture<Void> future = connectionManager.connect(host, port);
        
        // Verify that future is completed with exception
        assertTrue(future.isCompletedExceptionally(), "Future should be completed with exception");
        
        // After connection failure, it should be in RECONNECTING state (since it will try to reconnect)
        // Wait a moment for state to settle
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify state is either ERROR or RECONNECTING (both are valid after a failure)
        ConnectionState state = connectionManager.getState();
        assertTrue(state == ConnectionState.ERROR || state == ConnectionState.RECONNECTING, 
                "State should be ERROR or RECONNECTING, but was: " + state);
        
        // The error is wrapped in a CompletionException, so verify with the wrapped exception
        // Note that there might be multiple error calls (for initial connect and reconnect attempts)
        verify(listener, atLeastOnce()).onConnectionError(argThat(error -> 
            error instanceof CompletionException && 
            error.getCause() != null && 
            error.getCause().equals(exception)));
        
        // Verify that connection attempts were made
        // We can't easily predict exact number of connection attempts due to async nature
        verify(mockNetworkClient, atLeastOnce()).connect(host, port);
        
        // Check for at least one connection attempt
        verify(mockNetworkClient, atLeastOnce()).connect(anyString(), anyInt());
    }

    @Test
    public void testDisconnect() {
        // Setup
        String host = "localhost";
        int port = 8080;
        
        // Mock both connect and disconnect
        when(mockNetworkClient.connect(anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockNetworkClient.disconnect())
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // First connect
        connectionManager.connect(host, port);
        
        // Then disconnect
        CompletableFuture<Void> future = connectionManager.disconnect();
        
        // Verify
        assertTrue(future.isDone(), "Future should be completed");
        assertEquals(ConnectionState.DISCONNECTED, connectionManager.getState());
        verify(mockNetworkClient).disconnect();
    }

    @Test
    public void testIsConnected() {
        // Initially not connected
        assertFalse(connectionManager.isConnected());
        
        // Simulate connection
        when(mockNetworkClient.connect(anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        connectionManager.connect("localhost", 8080);
        
        // Should be connected
        assertTrue(connectionManager.isConnected());
    }

    @Test
    public void testIsRunning() {
        // Initially not running
        assertFalse(connectionManager.isRunning());
        
        // Simulate connection
        when(mockNetworkClient.connect(anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        connectionManager.connect("localhost", 8080);
        
        // Should be running
        assertTrue(connectionManager.isRunning());
    }

    @Test
    public void testAddListener() {
        ConnectionManager.ConnectionStateListener listener = mock(ConnectionManager.ConnectionStateListener.class);
        
        // Add listener
        connectionManager.addListener(listener);
        
        // Verify no exception
        assertDoesNotThrow(() -> {
            connectionManager.addListener(null);  // Should handle null gracefully
        });
    }

    @Test
    public void testRemoveListener() {
        ConnectionManager.ConnectionStateListener listener = mock(ConnectionManager.ConnectionStateListener.class);
        
        // Add and then remove listener
        connectionManager.addListener(listener);
        connectionManager.removeListener(listener);
        
        // Verify no exception
        assertDoesNotThrow(() -> {
            connectionManager.removeListener(null);  // Should handle null gracefully
        });
    }

    @Test
    public void testShutdown() {
        // Setup
        when(mockNetworkClient.shutdown())
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // Call the method
        CompletableFuture<Void> future = connectionManager.shutdown();
        
        // Verify - wait for the future to complete if it's not already done
        if (!future.isDone()) {
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Future should complete without exception");
            }
        }
        
        assertTrue(future.isDone(), "Future should be completed");
        assertEquals(ConnectionState.DISCONNECTED, connectionManager.getState());
        verify(mockNetworkClient).shutdown();
    }
}