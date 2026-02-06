package com.chatv2.client.network;

import com.chatv2.common.protocol.ChatMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Network client for TCP communication with server.
 */
public class NetworkClient {
    private static final Logger log = LoggerFactory.getLogger(NetworkClient.class);

    private EventLoopGroup workerGroup;
    private Channel channel;
    private Consumer<ChatMessage> messageHandler;
    private final ConcurrentHashMap<String, CompletableFuture<ChatMessage>> pendingRequests = new ConcurrentHashMap<>();

    public NetworkClient() {
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
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // Frame decoder/encoder
                        pipeline.addLast("frameDecoder",
                            new LengthFieldBasedFrameDecoder(10485760, 0, 4, 0, 0));
                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));

                        // String decoder/encoder
                        pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                        pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));

                        // Message handler
                        pipeline.addLast("clientHandler", new ClientHandler(messageHandler, pendingRequests));
                    }
                });

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
     * Sends a request to the server.
     */
    public CompletableFuture<ChatMessage> sendRequest(String request) {
        CompletableFuture<ChatMessage> responseFuture = new CompletableFuture<>();

        if (channel == null || !channel.isActive()) {
            responseFuture.completeExceptionally(new IllegalStateException("Not connected"));
            return responseFuture;
        }

        String requestId = java.util.UUID.randomUUID().toString();
        pendingRequests.put(requestId, responseFuture);

        String message = requestId + ":" + request;
        channel.writeAndFlush(message);

        log.debug("Sent request: {}", message);

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
