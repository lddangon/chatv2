package com.chatv2.client.network;

import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.common.protocol.BinaryMessageCodec;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client message handler for Netty.
 * Handles incoming ChatMessage objects from the server using binary protocol.
 */
public class ClientHandler extends SimpleChannelInboundHandler<ChatMessage> {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Consumer<ChatMessage> messageHandler;
    private final Map<String, CompletableFuture<ChatMessage>> pendingRequests;

    /**
     * Creates a new client handler.
     *
     * @param messageHandler the consumer for handling server-initiated messages
     * @param pendingRequests the map of pending request message IDs to futures
     */
    public ClientHandler(Consumer<ChatMessage> messageHandler, Map<String, CompletableFuture<ChatMessage>> pendingRequests) {
        this.messageHandler = messageHandler;
        this.pendingRequests = pendingRequests;
    }

    /**
     * Handles incoming ChatMessage objects.
     * Routes messages to pending requests or to the message handler.
     *
     * @param ctx the channel handler context
     * @param msg the received chat message
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            log.debug("Received message: type={}, id={}, payloadSize={}",
                    msg.getMessageType(), msg.getMessageId(), msg.getPayload().length);

            // Use message ID to match requests with responses
            String messageId = msg.getMessageId().toString();

            // Check if this is a response to a pending request
            CompletableFuture<ChatMessage> responseFuture = pendingRequests.remove(messageId);
            if (responseFuture != null) {
                log.debug("Completing pending request: {}", messageId);
                responseFuture.complete(msg);
            } else {
                // This is a server-initiated message
                if (messageHandler != null) {
                    log.debug("Handling server-initiated message: type={}", msg.getMessageType());
                    messageHandler.accept(msg);
                }
            }
        } catch (Exception e) {
            log.error("Error processing incoming message: {}", msg.getMessageId(), e);
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
