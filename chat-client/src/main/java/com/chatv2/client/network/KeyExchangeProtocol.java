package com.chatv2.client.network;

import com.chatv2.common.crypto.KeyManager;
import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.exception.NetworkException;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.encryption.api.EncryptionPlugin;
import com.chatv2.encryption.rsa.RsaEncryptionPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Protocol for exchanging encryption keys between client and server.
 * Implements RSA+AES key exchange using server's public key.
 */
public class KeyExchangeProtocol {
    private static final Logger log = LoggerFactory.getLogger(KeyExchangeProtocol.class);
    
    private static final int TIMEOUT_SECONDS = 5;
    private static final int AES_KEY_SIZE = 256;
    private static final String AES_ALGORITHM = "AES";
    
    private final NetworkClient networkClient;
    private final RsaEncryptionPlugin rsaPlugin;
    private final ObjectMapper objectMapper;
    private final ReentrantLock exchangeLock;
    
    private byte[] sessionKey;
    private PublicKey serverPublicKey;
    private volatile boolean keyExchanged;
    
    // Map for storing pending key exchange responses
    private final ConcurrentHashMap<String, CompletableFuture<ChatMessage>> pendingResponses;
    
    /**
     * Creates a new key exchange protocol handler.
     *
     * @param networkClient the network client for communication
     * @param rsaPlugin the RSA encryption plugin
     */
    public KeyExchangeProtocol(NetworkClient networkClient, RsaEncryptionPlugin rsaPlugin) {
        if (networkClient == null) {
            throw new IllegalArgumentException("NetworkClient cannot be null");
        }
        if (rsaPlugin == null) {
            throw new IllegalArgumentException("RsaEncryptionPlugin cannot be null");
        }
        
        this.networkClient = networkClient;
        this.rsaPlugin = rsaPlugin;
        this.objectMapper = new ObjectMapper();
        this.exchangeLock = new ReentrantLock();
        this.keyExchanged = false;
        this.pendingResponses = new ConcurrentHashMap<>();
        
        log.info("KeyExchangeProtocol initialized");
    }
    
    /**
     * Performs the complete key exchange protocol with the server.
     * 
     * Protocol steps:
     * 1. Request server's RSA public key (AUTH_HANDSHAKE_REQ/RES)
     * 2. Generate AES-256 session key
     * 3. Encrypt session key with RSA public key
     * 4. Send encrypted key to server (AUTH_KEY_EXCHANGE_REQ/RES)
     * 5. Receive confirmation from server
     *
     * @return CompletableFuture containing the session key bytes
     */
    public CompletableFuture<byte[]> performKeyExchange() {
        log.info("Starting key exchange protocol");
        
        return CompletableFuture.supplyAsync(() -> {
            exchangeLock.lock();
            try {
                if (keyExchanged) {
                    log.warn("Key already exchanged, returning existing session key");
                    return sessionKey;
                }
                
                // Step 1: Request server's RSA public key
                log.info("Step 1: Requesting server's RSA public key");
                PublicKey publicKey = requestServerPublicKey().join();
                this.serverPublicKey = publicKey;
                log.debug("Received server's RSA public key");
                
                // Step 2: Generate AES-256 session key
                log.info("Step 2: Generating AES-256 session key");
                byte[] aesKey = generateAesSessionKey();
                log.debug("Generated AES-256 session key ({} bytes)", aesKey.length);
                
                // Step 3: Encrypt session key with RSA
                log.info("Step 3: Encrypting session key with RSA");
                byte[] encryptedKey = encryptSessionKey(aesKey).join();
                log.debug("Encrypted session key ({} bytes)", encryptedKey.length);
                
                // Step 4: Send encrypted key to server
                log.info("Step 4: Sending encrypted session key to server");
                sendEncryptedKey(encryptedKey).join();
                log.debug("Encrypted session key sent to server");
                
                // Step 5: Receive confirmation from server
                log.info("Step 5: Receiving confirmation from server");
                receiveConfirmation().join();
                log.debug("Received confirmation from server");
                
                // Store session key and mark as exchanged
                this.sessionKey = aesKey;
                this.keyExchanged = true;
                
                log.info("Key exchange protocol completed successfully");
                return sessionKey;
                
            } catch (Exception e) {
                log.error("Key exchange protocol failed", e);
                keyExchanged = false;
                throw new NetworkException("Key exchange failed: " + e.getMessage(), e);
            } finally {
                exchangeLock.unlock();
            }
        });
    }
    
    /**
     * Requests the server's RSA public key.
     * Sends AUTH_HANDSHAKE_REQ and waits for AUTH_HANDSHAKE_RES.
     *
     * @return CompletableFuture containing the server's public key
     */
    private CompletableFuture<PublicKey> requestServerPublicKey() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create request message
                String requestId = UUID.randomUUID().toString();
                ChatMessage request = ChatMessage.createUnencrypted(ProtocolMessageType.AUTH_HANDSHAKE_REQ, new byte[0]);
                
                log.debug("Sending AUTH_HANDSHAKE_REQ: {}", request);
                
                // Register response handler
                CompletableFuture<ChatMessage> responseFuture = new CompletableFuture<>();
                pendingResponses.put(requestId, responseFuture);
                
                // Send request
                networkClient.getChannel().writeAndFlush(request).await();
                
                // Wait for response with timeout
                ChatMessage response;
                try {
                    response = responseFuture.orTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS).join();
                } catch (Exception e) {
                    if (e.getCause() instanceof TimeoutException) {
                        throw new NetworkException("Timeout while requesting server public key", e);
                    }
                    throw e;
                }
                pendingResponses.remove(requestId);
                
                log.debug("Received AUTH_HANDSHAKE_RES: {}", response);
                
                // Validate response type
                if (response.getMessageType() != ProtocolMessageType.AUTH_HANDSHAKE_RES) {
                    throw new NetworkException("Unexpected response type: " + response.getMessageType());
                }
                
                // Extract RSA public key from payload (Base64 encoded)
                String publicKeyBase64 = new String(response.getPayload());
                
                // Decode public key
                PublicKey publicKey = KeyManager.publicKeyFromBase64(publicKeyBase64);
                
                return publicKey;
            } catch (Exception e) {
                throw new NetworkException("Failed to request server public key: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Generates a new AES-256 session key.
     *
     * @return the generated session key bytes
     */
    private byte[] generateAesSessionKey() {
        try {
            javax.crypto.SecretKey aesKey = KeyManager.generateAesKey(AES_KEY_SIZE);
            byte[] keyBytes = aesKey.getEncoded();
            
            if (keyBytes.length != AES_KEY_SIZE / 8) {
                throw new NetworkException("Invalid AES key size: " + keyBytes.length);
            }
            
            return keyBytes;
        } catch (Exception e) {
            throw new NetworkException("Failed to generate AES session key: " + e.getMessage(), e);
        }
    }
    
    /**
     * Encrypts the session key using the server's RSA public key.
     *
     * @param sessionKey the session key to encrypt
     * @return CompletableFuture containing the encrypted session key
     */
    private CompletableFuture<byte[]> encryptSessionKey(byte[] sessionKey) {
        if (serverPublicKey == null) {
            return CompletableFuture.failedFuture(new NetworkException("Server public key not available"));
        }
        
        return rsaPlugin.encrypt(sessionKey, serverPublicKey)
            .thenApply(EncryptionResult::ciphertext)
            .exceptionally(e -> {
                log.error("Failed to encrypt session key", e);
                throw new NetworkException("Failed to encrypt session key", e);
            });
    }
    
    /**
     * Sends the encrypted session key to the server.
     * Sends AUTH_KEY_EXCHANGE_REQ with the encrypted key in payload.
     *
     * @param encryptedKey the encrypted session key
     * @return CompletableFuture that completes when the key is sent
     */
    private CompletableFuture<Void> sendEncryptedKey(byte[] encryptedKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Encode encrypted key to Base64 for transmission
                String encodedKey = Base64.getEncoder().encodeToString(encryptedKey);
                byte[] payload = encodedKey.getBytes();
                
                // Create request message
                String requestId = UUID.randomUUID().toString();
                ChatMessage request = ChatMessage.createUnencrypted(ProtocolMessageType.AUTH_KEY_EXCHANGE_REQ, payload);
                
                log.debug("Sending AUTH_KEY_EXCHANGE_REQ with encrypted key");
                
                // Register response handler
                CompletableFuture<ChatMessage> responseFuture = new CompletableFuture<>();
                pendingResponses.put(requestId, responseFuture);
                
                // Send request
                networkClient.getChannel().writeAndFlush(request).await();
                
                // Wait for response with timeout
                ChatMessage response;
                try {
                    response = responseFuture.orTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS).join();
                } catch (Exception e) {
                    if (e.getCause() instanceof TimeoutException) {
                        throw new NetworkException("Timeout while sending encrypted key", e);
                    }
                    throw e;
                }
                pendingResponses.remove(requestId);
                
                log.debug("Received AUTH_KEY_EXCHANGE_RES: {}", response);
                
                // Validate response type
                if (response.getMessageType() != ProtocolMessageType.AUTH_KEY_EXCHANGE_RES) {
                    throw new NetworkException("Unexpected response type: " + response.getMessageType());
                }
                
                // Check success flag (payload contains "success" or error message)
                String result = new String(response.getPayload());
                if (!"success".equalsIgnoreCase(result)) {
                    throw new NetworkException("Server rejected key exchange: " + result);
                }
                
            } catch (Exception e) {
                throw new NetworkException("Failed to send encrypted key: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Receives confirmation from the server.
     * This is handled in sendEncryptedKey method, but kept for protocol completeness.
     *
     * @return CompletableFuture that completes when confirmation is received
     */
    private CompletableFuture<Void> receiveConfirmation() {
        // Confirmation is already received in sendEncryptedKey
        // This method is kept for protocol completeness
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Gets the current session key.
     *
     * @return the session key bytes, or null if not exchanged
     */
    public byte[] getSessionKey() {
        return sessionKey;
    }
    
    /**
     * Gets the session key as a SecretKeySpec for encryption operations.
     *
     * @return the session key as SecretKeySpec, or null if not exchanged
     */
    public javax.crypto.SecretKey getSessionKeySpec() {
        if (sessionKey == null) {
            return null;
        }
        return new SecretKeySpec(sessionKey, AES_ALGORITHM);
    }
    
    /**
     * Gets the server's RSA public key.
     *
     * @return the public key, or null if not received
     */
    public PublicKey getServerPublicKey() {
        return serverPublicKey;
    }
    
    /**
     * Checks if the key exchange has been completed.
     *
     * @return true if key exchange is complete, false otherwise
     */
    public boolean isKeyExchanged() {
        return keyExchanged && sessionKey != null;
    }
    
    /**
     * Resets the key exchange state.
     * Clears the session key and marks exchange as incomplete.
     */
    public void reset() {
        exchangeLock.lock();
        try {
            this.sessionKey = null;
            this.serverPublicKey = null;
            this.keyExchanged = false;
            pendingResponses.clear();
            log.info("Key exchange state reset");
        } finally {
            exchangeLock.unlock();
        }
    }
    
    /**
     * Handles an incoming message that might be part of the key exchange protocol.
     * This method should be called by the message handler for all incoming messages.
     *
     * @param message the incoming message
     */
    public void handleIncomingMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        
        ProtocolMessageType type = message.getMessageType();
        
        // Check if this is a key exchange response
        if (message.getMessageType() == ProtocolMessageType.AUTH_HANDSHAKE_RES ||
            message.getMessageType() == ProtocolMessageType.AUTH_KEY_EXCHANGE_RES) {
            
            log.debug("Handling key exchange response: {}", type);
            
            // Find and complete the pending future
            for (String requestId : pendingResponses.keySet()) {
                CompletableFuture<ChatMessage> future = pendingResponses.get(requestId);
                if (future != null && !future.isDone()) {
                    future.complete(message);
                    log.debug("Completed pending request: {}", requestId);
                    break;
                }
            }
        }
    }
    
    /**
     * Cleans up resources used by the key exchange protocol.
     */
    public void shutdown() {
        exchangeLock.lock();
        try {
            reset();
            log.info("KeyExchangeProtocol shutdown complete");
        } finally {
            exchangeLock.unlock();
        }
    }
}
