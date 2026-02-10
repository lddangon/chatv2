package com.chatv2.client.network;

import com.chatv2.common.crypto.EncryptionResult;
import com.chatv2.common.exception.ChatException;
import com.chatv2.common.protocol.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Netty handler for automatic message encryption and decryption on the client side.
 * Handles encryption of outgoing messages and decryption of incoming messages.
 */
@io.netty.channel.ChannelHandler.Sharable
public class EncryptionHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(EncryptionHandler.class);
    
    private volatile Key sessionKey;
    private volatile boolean encryptionEnabled = false;
    
    // AES-GCM sizes
    private static final int IV_SIZE = 12; // bytes
    private static final int TAG_SIZE = 16; // bytes
    
    // Reference to the client's encryption functionality
    private final EncryptionService encryptionService;

    /**
     * Creates a new EncryptionHandler.
     *
     * @param encryptionService the encryption service
     */
    public EncryptionHandler(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Decrypts incoming messages before passing to next handler.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ChatMessage chatMsg) {
            try {
                if (chatMsg.isEncrypted() && encryptionEnabled) {
                    log.debug("Decrypting incoming message: {}", chatMsg.getMessageId());
                    
                    // Decrypt payload
                    byte[] decryptedPayload = decryptPayload(chatMsg.getPayload());
                    
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
                    // Pass through unencrypted message or if encryption not enabled
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
                    byte[] encryptedPayload = encryptPayload(chatMsg.getPayload());
                    
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
        // Если encryptionEnabled false ИЛИ sessionKey null - не шифровать
        if (!encryptionEnabled || sessionKey == null) {
            return false;
        }
        // Не шифровать сообщения handshake
        return !message.getMessageType().name().startsWith("AUTH_HANDSHAKE");
    }

    /**
     * Encrypts payload using session key.
     *
     * @param payload the payload to encrypt
     * @return encrypted payload bytes (IV + TAG + ciphertext)
     */
    private byte[] encryptPayload(byte[] payload) {
        if (sessionKey == null) {
            throw new IllegalStateException("Session key not set");
        }

        try {
            // Encrypt using encryption service
            EncryptionResult result = encryptionService.encrypt(payload, sessionKey);

            // Combine IV, TAG, and ciphertext
            return result.toCombinedArray();
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new ChatException(ChatException.ENCRYPTION_ERROR,
                "Failed to encrypt payload", e);
        }
    }

    /**
     * Decrypts payload using session key.
     *
     * @param payload the encrypted payload (IV + TAG + ciphertext)
     * @return decrypted payload bytes
     */
    private byte[] decryptPayload(byte[] payload) {
        if (sessionKey == null) {
            throw new IllegalStateException("Session key not set");
        }

        if (payload.length < IV_SIZE + TAG_SIZE) {
            throw new IllegalArgumentException("Encrypted payload too short");
        }

        try {
            // Split payload into IV, TAG, and ciphertext
            EncryptionResult result = EncryptionResult.fromCombinedArray(payload, IV_SIZE, TAG_SIZE);

            // Decrypt using encryption service
            return encryptionService.decrypt(
                result.ciphertext(), result.iv(), result.tag(), sessionKey
            );
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new ChatException(ChatException.ENCRYPTION_ERROR,
                "Failed to decrypt payload", e);
        }
    }

    /**
     * Sets the session key for encryption/decryption.
     *
     * @param sessionKey the session key (as SecretKey)
     */
    public void setSessionKey(Key sessionKey) {
        this.sessionKey = sessionKey;
        this.encryptionEnabled = (sessionKey != null);
        log.debug("Session key updated. Encryption enabled: {}", encryptionEnabled);
    }

    /**
     * Clears the session key (called on disconnect).
     */
    public void clearSessionKey() {
        this.sessionKey = null;
        this.encryptionEnabled = false;
        log.debug("Session key cleared. Encryption disabled");
    }

    /**
     * Checks if encryption is enabled.
     *
     * @return true if encryption is enabled
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * Clears session key on channel close.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clearSessionKey();
        log.debug("Encryption disabled on channel close: {}", ctx.channel());
        super.channelInactive(ctx);
    }

    /**
     * Clears session key on exception.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ChatException && cause.getMessage().contains("decrypt")) {
            log.warn("Decryption error, clearing session key: {}", ctx.channel());
            clearSessionKey();
        }
        super.exceptionCaught(ctx, cause);
    }

    /**
     * Interface for encryption service.
     */
    public interface EncryptionService {
        /**
         * Encrypts data using the provided key.
         *
         * @param plaintext data to encrypt
         * @param key       encryption key
         * @return encryption result
         */
        EncryptionResult encrypt(byte[] plaintext, Key key);

        /**
         * Decrypts data using the provided key.
         *
         * @param ciphertext encrypted data
         * @param iv         initialization vector
         * @param tag        authentication tag
         * @param key        decryption key
         * @return decrypted data
         */
        byte[] decrypt(byte[] ciphertext, byte[] iv, byte[] tag, Key key);
    }
}
