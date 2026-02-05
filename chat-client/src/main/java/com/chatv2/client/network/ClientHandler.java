package com.chatv2.client.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client message handler for Netty.
 */
public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Consumer<String> messageHandler;
    private final Map<String, CompletableFuture<String>> pendingRequests;

    public ClientHandler(Consumer<String> messageHandler, Map<String, CompletableFuture<String>> pendingRequests) {
        this.messageHandler = messageHandler;
        this.pendingRequests = pendingRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            log.debug("Received message: {}", msg);

            // Parse message
            String[] parts = msg.split(":", 2);
            if (parts.length >= 2) {
                String requestId = parts[0];
                String payload = parts[1];

                // Check if this is a response to a pending request
                CompletableFuture<String> responseFuture = pendingRequests.remove(requestId);
                if (responseFuture != null) {
                    responseFuture.complete(payload);
                } else {
                    // This is a server-initiated message
                    if (messageHandler != null) {
                        messageHandler.accept(msg);
                    }
                }
            } else {
                // No request ID, treat as server message
                if (messageHandler != null) {
                    messageHandler.accept(msg);
                }
            }
        } catch (Exception e) {
            log.error("Error processing incoming message", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Channel activated: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Channel deactivated: {}", ctx.channel().remoteAddress());

        // Complete all pending requests with exception
        pendingRequests.forEach((id, future) ->
            future.completeExceptionally(new RuntimeException("Connection closed")));
        pendingRequests.clear();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in client handler", cause);
        ctx.close();
    }
}
