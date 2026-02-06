package com.chatv2.server.pipeline;

import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.exception.ChatException;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.server.manager.EncryptionPluginManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty handler for automatic message encryption and decryption.
 * Integrates with EncryptionPluginManager to handle message security.
 */
@io.netty.channel.ChannelHandler.Sharable
public class EncryptionHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(EncryptionHandler.class);

    private final EncryptionPluginManager pluginManager;
    private final Map<Channel, byte[]> sessionKeys = new ConcurrentHashMap<>();

    // AES-GCM sizes
    private static final int IV_SIZE = 12; // bytes
    private static final int TAG_SIZE = 16; // bytes

    /**
     * Creates a new EncryptionHandler.
     *
     * @param pluginManager the encryption plugin manager
     * @param sessionManager the session manager (unused - kept for compatibility)
     */
    public EncryptionHandler(EncryptionPluginManager pluginManager, SessionManager sessionManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Decrypts incoming messages before passing to next handler.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ChatMessage chatMsg) {
            try {
                if (chatMsg.isEncrypted()) {
                    log.debug("Decrypting incoming message: {}", chatMsg.getMessageId());
                    
                    // Decrypt payload
                    byte[] decryptedPayload = decryptPayload(
                        ctx.channel(),
                        chatMsg.getPayload()
                    );
                    
                    // Create new message with decrypted payload and cleared encrypted flag
                    ChatMessage decryptedMsg = new ChatMessage(
                        chatMsg.getMessageType(),
                        (byte) (chatMsg.getFlags() & ~ChatMessage.FLAG_ENCRYPTED),
                        chatMsg.getMessageId(),
                        chatMsg.getTimestamp(),
                        decryptedPayload
                    );
                    
                    log.debug("Message decrypted successfully");
                    ctx.fireChannelRead(decryptedMsg);
                } else {
                    // Pass through unencrypted message
                    ctx.fireChannelRead(chatMsg);
                }
            } catch (Exception e) {
                log.error("Failed to decrypt message: {}", chatMsg.getMessageId(), e);
                throw new ChatException(ChatException.ENCRYPTION_ERROR,
                    "Failed to decrypt message", e);
            }
        } else {
            // Pass through non-ChatMessage objects
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Encrypts outgoing messages.
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ChatMessage chatMsg) {
            try {
                if (shouldEncrypt(chatMsg)) {
                    log.debug("Encrypting outgoing message: {}", chatMsg.getMessageId());
                    
                    // Encrypt payload
                    byte[] encryptedPayload = encryptPayload(
                        ctx.channel(),
                        chatMsg.getPayload()
                    );
                    
                    // Create new message with encrypted payload and set encrypted flag
                    ChatMessage encryptedMsg = new ChatMessage(
                        chatMsg.getMessageType(),
                        (byte) (chatMsg.getFlags() | ChatMessage.FLAG_ENCRYPTED),
                        chatMsg.getMessageId(),
                        chatMsg.getTimestamp(),
                        encryptedPayload
                    );
                    
                    log.debug("Message encrypted successfully");
                    ctx.write(encryptedMsg, promise);
                } else {
                    // Pass through unencrypted message
                    ctx.write(chatMsg, promise);
                }
            } catch (Exception e) {
                log.error("Failed to encrypt message: {}", chatMsg.getMessageId(), e);
                throw new ChatException(ChatException.ENCRYPTION_ERROR,
                    "Failed to encrypt message", e);
            }
        } else {
            // Pass through non-ChatMessage objects
            ctx.write(msg, promise);
        }
    }

    /**
     * Determines if a message should be encrypted.
     */
    private boolean shouldEncrypt(ChatMessage message) {
        // Don't encrypt if plugin manager or session key is not set
        if (!pluginManager.isEncryptionEnabled()) {
            return false;
        }
        
        // Check if encryption is required for this message type
        // Some messages (like handshake) are sent unencrypted
        return !message.getMessageType().name().startsWith("AUTH_HANDSHAKE");
    }

    /**
     * Encrypts payload using active encryption plugin.
     *
     * @param channel the channel (for session key lookup)
     * @param payload  the payload to encrypt
     * @return encrypted payload bytes (IV + TAG + ciphertext)
     */
    private byte[] encryptPayload(Channel channel, byte[] payload) {
        try {
            // Get session key for channel
            Key sessionKey = getSessionKey(channel);
            if (sessionKey == null) {
                throw new IllegalStateException("No session key set for channel");
            }

            // Set session key for encryption
            pluginManager.setActiveSessionKey(sessionKey);

            // Encrypt using plugin manager
            CompletableFuture<EncryptionResult> future = pluginManager.encrypt(payload);
            EncryptionResult result = future.join();

            // Combine IV, TAG, and ciphertext
            return result.toCombinedArray();
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new ChatException(ChatException.ENCRYPTION_ERROR,
                "Failed to encrypt payload", e);
        }
    }

    /**
     * Decrypts payload using active encryption plugin.
     *
     * @param channel the channel (for session key lookup)
     * @param payload  the encrypted payload (IV + TAG + ciphertext)
     * @return decrypted payload bytes
     */
    private byte[] decryptPayload(Channel channel, byte[] payload) {
        try {
            if (payload.length < IV_SIZE + TAG_SIZE) {
                throw new IllegalArgumentException("Encrypted payload too short");
            }

            // Get session key for channel
            Key sessionKey = getSessionKey(channel);
            if (sessionKey == null) {
                throw new IllegalStateException("No session key set for channel");
            }

            // Set session key for decryption
            pluginManager.setActiveSessionKey(sessionKey);

            // Split payload into IV, TAG, and ciphertext
            EncryptionResult result = EncryptionResult.fromCombinedArray(payload, IV_SIZE, TAG_SIZE);

            // Decrypt using plugin manager
            CompletableFuture<byte[]> future = pluginManager.decrypt(
                result.ciphertext(), result.iv(), result.tag()
            );
            return future.join();
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new ChatException(ChatException.ENCRYPTION_ERROR,
                "Failed to decrypt payload", e);
        }
    }

    /**
     * Gets the session key for a channel.
     *
     * @param channel the channel
     * @return session key or null if not set
     */
    private Key getSessionKey(Channel channel) {
        byte[] keyBytes = sessionKeys.get(channel);
        if (keyBytes == null) {
            return null;
        }
        
        // Convert bytes to SecretKey
        return new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Sets the session key for a channel.
     *
     * @param channel    the channel
     * @param sessionKey the session key (as SecretKey)
     */
    public void setSessionKey(Channel channel, Key sessionKey) {
        if (sessionKey == null) {
            sessionKeys.remove(channel);
            log.debug("Session key removed for channel: {}", channel);
        } else {
            sessionKeys.put(channel, sessionKey.getEncoded());
            log.debug("Session key set for channel: {}", channel);
        }
    }

    /**
     * Removes the session key for a channel (called on channel close).
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionKeys.remove(ctx.channel());
        log.debug("Session key cleared for inactive channel: {}", ctx.channel());
        super.channelInactive(ctx);
    }

    /**
     * Clears session key on exception.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ChatException && cause.getMessage().contains("decrypt")) {
            log.warn("Decryption error, clearing session key: {}", ctx.channel());
            sessionKeys.remove(ctx.channel());
        }
        super.exceptionCaught(ctx, cause);
    }
}
