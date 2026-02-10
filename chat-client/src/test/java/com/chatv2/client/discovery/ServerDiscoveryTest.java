package com.chatv2.client.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServerDiscovery class.
 * Focuses on testing the sendDiscoveryRequest method to ensure
 * correct format string without MissingFormatArgumentException.
 */
@ExtendWith(MockitoExtension.class)
public class ServerDiscoveryTest {

    private ServerDiscovery serverDiscovery;
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int MULTICAST_PORT = 9000;
    private static final int TIMEOUT_SECONDS = 5;

    @BeforeEach
    public void setUp() {
        serverDiscovery = new ServerDiscovery(MULTICAST_ADDRESS, MULTICAST_PORT, TIMEOUT_SECONDS);
    }

    @Test
    public void testSendDiscoveryRequestFormat() {
        // This test verifies that the format string has the correct number of arguments
        // to prevent MissingFormatArgumentException
        
        // Create a mock multicast socket
        MulticastSocket mockSocket = mock(MulticastSocket.class);
        
        try {
            // Use reflection to access the private method
            var method = ServerDiscovery.class.getDeclaredMethod("sendDiscoveryRequest");
            method.setAccessible(true);
            
            // Set up the mock socket using reflection
            var socketField = ServerDiscovery.class.getDeclaredField("multicastSocket");
            socketField.setAccessible(true);
            socketField.set(serverDiscovery, mockSocket);
            
            // Call the method under test
            method.invoke(serverDiscovery);
            
            // Verify the socket.send method was called
            verify(mockSocket, times(1)).send(any(DatagramPacket.class));
            
            // Capture the packet to verify its contents
            verify(mockSocket).send(argThat(packet -> {
                byte[] data = packet.getData();
                String message = new String(data, 0, packet.getLength());
                
                // Verify the format of the message
                assertTrue(message.startsWith("SERVICE_DISCOVERY_REQ:"));
                
                // Split the message to check all parts are present
                String[] parts = message.split(":");
                assertEquals(5, parts.length); // Should have 5 parts: SERVICE_DISCOVERY_REQ, clientId, version, platform, encryption
                
                // Verify that all parts are non-empty
                for (String part : parts) {
                    assertFalse(part.isEmpty());
                }
                
                return true;
            }));
            
        } catch (Exception e) {
            fail("Exception during test execution: " + e.getMessage());
        }
    }
    
    @Test
    public void testConstructor() {
        // Verify the constructor sets values correctly
        ServerDiscovery discovery = new ServerDiscovery("test.address", 1234, 10);
        assertEquals("test.address", getMulticastAddress(discovery));
        assertEquals(1234, getMulticastPort(discovery));
        assertEquals(10, getTimeoutSeconds(discovery));
    }
    
    @Test
    public void testGetDiscoveredServersInitiallyEmpty() {
        // Initially, the discovered servers list should be empty
        assertTrue(serverDiscovery.getDiscoveredServers().isEmpty());
    }
    
    @Test
    public void testListenerManagement() {
        // Test adding and removing listeners
        var listener = mock(java.util.function.Consumer.class);
        
        serverDiscovery.addListener(listener);
        serverDiscovery.removeListener(listener);
        
        // We can't easily verify the internal state without reflection
        // but we can at least ensure no exceptions are thrown
        assertDoesNotThrow(() -> {
            serverDiscovery.addListener(listener);
            serverDiscovery.removeListener(listener);
        });
    }
    
    // Helper methods to access private fields without reflection libraries
    private String getMulticastAddress(ServerDiscovery discovery) {
        try {
            var field = ServerDiscovery.class.getDeclaredField("multicastAddress");
            field.setAccessible(true);
            return (String) field.get(discovery);
        } catch (Exception e) {
            return null;
        }
    }
    
    private int getMulticastPort(ServerDiscovery discovery) {
        try {
            var field = ServerDiscovery.class.getDeclaredField("multicastPort");
            field.setAccessible(true);
            return (int) field.get(discovery);
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int getTimeoutSeconds(ServerDiscovery discovery) {
        try {
            var field = ServerDiscovery.class.getDeclaredField("timeoutSeconds");
            field.setAccessible(true);
            return (int) field.get(discovery);
        } catch (Exception e) {
            return -1;
        }
    }
}