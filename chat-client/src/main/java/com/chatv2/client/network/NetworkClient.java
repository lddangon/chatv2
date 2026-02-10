package com.chatv2.client.network;

import com.chatv2.common.protocol.BinaryMessageCodec;
import com.chatv2.common.protocol.ChatMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Network client for TCP communication with server using binary protocol.
 */
public class NetworkClient {
    private static final Logger log = LoggerFactory.getLogger(NetworkClient.class);

    private EventLoopGroup workerGroup;
    private Channel channel;
    private Consumer<ChatMessage> messageHandler;
    private final ConcurrentHashMap<String, CompletableFuture<ChatMessage>> pendingRequests = new ConcurrentHashMap<>();
    private final com.chatv2.client.network.EncryptionHandler encryptionHandler;

    public NetworkClient() {
        // Use AES-256-GCM for authenticated encryption
        this.encryptionHandler = new com.chatv2.client.network.EncryptionHandler(new EncryptionHandler.EncryptionService() {
            @Override
            public com.chatv2.common.crypto.EncryptionResult encrypt(byte[] plaintext, java.security.Key key) {
                try {
                    return com.chatv2.common.crypto.AesGcmCrypto.encrypt(plaintext, key);
                } catch (Exception e) {
                    throw new RuntimeException("Encryption failed", e);
                }
            }

            @Override
            public byte[] decrypt(byte[] ciphertext, byte[] iv, byte[] tag, java.security.Key key) {
                try {
                    return com.chatv2.common.crypto.AesGcmCrypto.decrypt(ciphertext, iv, tag, key);
                } catch (Exception e) {
                    throw new RuntimeException("Decryption failed", e);
                }
            }
        });
    }

    /**
     * Connects to the server.
     */
    public CompletableFuture<Void> connect(String host, int port) {
        CompletableFuture<Void> connectFuture = new CompletableFuture<>();

        try {
            workerGroup = new NioEventLoopGroup();

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ClientInitializer(null, new ClientHandler(messageHandler, pendingRequests), encryptionHandler));

            // Connect to server
            ChannelFuture future = bootstrap.connect(host, port);
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    channel = channelFuture.channel();
                    log.info("Connected to {}:{}", host, port);
                    connectFuture.complete(null);
                } else {
                    log.error("Failed to connect to {}:{}", host, port, channelFuture.cause());
                    connectFuture.completeExceptionally(channelFuture.cause());
                }
            });

        } catch (Exception e) {
            log.error("Failed to initialize network client", e);
            connectFuture.completeExceptionally(e);
        }

        return connectFuture;
    }

    /**
     * Disconnects from the server.
     */
    public CompletableFuture<Void> disconnect() {
        CompletableFuture<Void> disconnectFuture = new CompletableFuture<>();

        if (channel != null && channel.isActive()) {
            channel.close().addListener((ChannelFutureListener) channelFuture -> {
                if (workerGroup != null) {
                    workerGroup.shutdownGracefully();
                }
                log.info("Disconnected from server");
                disconnectFuture.complete(null);
            });
        } else {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            disconnectFuture.complete(null);
        }

        return disconnectFuture;
    }

    /**
     * Sends a binary message to the server.
     *
     * @param request the ChatMessage to send
     * @return a CompletableFuture that will complete with the response message
     */
    public CompletableFuture<ChatMessage> sendRequest(ChatMessage request) {
        CompletableFuture<ChatMessage> responseFuture = new CompletableFuture<>();

        if (channel == null || !channel.isActive()) {
            responseFuture.completeExceptionally(new IllegalStateException("Not connected"));
            return responseFuture;
        }

        String requestId = request.getMessageId().toString();
        pendingRequests.put(requestId, responseFuture);

        channel.writeAndFlush(request);

        log.debug("Sent request: type={}, id={}", request.getMessageType(), requestId);

        return responseFuture;
    }

    /**
     * Sets a handler for incoming messages.
     */
    public void setMessageHandler(Consumer<ChatMessage> handler) {
        this.messageHandler = handler;
    }

    /**
     * Checks if connected.
     */
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    /**
     * Gets the channel.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Shuts down the client.
     */
    public CompletableFuture<Void> shutdown() {
        log.info("Shutting down network client");
        pendingRequests.clear();
        return disconnect();
    }
}
