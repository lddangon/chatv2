package com.chatv2.client.network;

import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.client.network.EncryptionHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side channel initializer.
 * Configures the channel pipeline for binary message processing.
 */
public class ClientInitializer extends ChannelInitializer<SocketChannel> {
    private static final Logger log = LoggerFactory.getLogger(ClientInitializer.class);

    private final SslContext sslContext;
    private final ClientHandler clientHandler;
    private final EncryptionHandler encryptionHandler;

    /**
     * Creates a new client channel initializer.
     *
     * @param sslContext the SSL context (null if SSL is disabled)
     * @param clientHandler the client message handler
     * @param encryptionHandler the encryption handler
     */
    public ClientInitializer(SslContext sslContext, ClientHandler clientHandler, EncryptionHandler encryptionHandler) {
        this.sslContext = sslContext;
        this.clientHandler = clientHandler;
        this.encryptionHandler = encryptionHandler;
    }

    /**
     * Initializes the channel pipeline.
     * Configures handlers for SSL, framing, message encoding/decoding, encryption, and business logic.
     *
     * @param ch the socket channel to initialize
     * @throws Exception if initialization fails
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        log.debug("Initializing client channel: {}", ch.remoteAddress());

        ChannelPipeline pipeline = ch.pipeline();

        // Add SSL handler (if SSL context is provided)
        if (sslContext != null) {
            pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
        }

        // Add binary message codec (implements PROTOCOL_SPEC.md)
        // - Parses PacketHeader (28 bytes) including payloadLength field
        // - Validates payload + checksum based on payloadLength
        // - Converts between byte[] and ChatMessage objects
        // Note: No separate LengthFieldBasedFrameDecoder needed - BinaryMessageCodec
        // implements proper framing according to the protocol specification
        pipeline.addLast("messageCodec", new BinaryMessageCodec());

        // Add encryption handler (decrypts incoming, encrypts outgoing)
        // Handles AES-GCM encryption/decryption of message payloads
        pipeline.addLast("encryptionHandler", encryptionHandler);

        // Add client handler
        // Handles business logic for incoming chat messages
        pipeline.addLast("clientHandler", clientHandler);

        log.debug("Client pipeline initialized successfully");
    }
}
