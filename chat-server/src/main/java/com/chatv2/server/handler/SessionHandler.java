package com.chatv2.server.handler;

import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Session handler for validating sessions.
 * Works with ChatMessage instead of String.
 */
public class SessionHandler extends SimpleChannelInboundHandler<ChatMessage> {
    private static final Logger log = LoggerFactory.getLogger(SessionHandler.class);
    private final SessionManager sessionManager;

    public SessionHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            log.debug("Received session message: type={}, messageId={}", msg.getMessageType(), msg.getMessageId());

            // Convert payload to string (temporary - will use proper protocol parsing)
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);

            if (payload.contains("SESSION_VALIDATE")) {
                // Handle session validation
                String[] parts = payload.split(":");
                if (parts.length >= 2) {
                    String token = parts[1];

                    sessionManager.validateSession(token)
                        .thenAccept(session -> {
                            String response = "SESSION_VALIDATE_RES:SUCCESS:" + session.userId();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.SESSION_VALIDATE_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                        })
                        .exceptionally(ex -> {
                            String response = "SESSION_VALIDATE_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.SESSION_VALIDATE_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing session message", e);
            String response = "ERROR:" + e.getMessage();
            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(responseMsg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Session handler exception", cause);
        String response = "ERROR:" + cause.getMessage();
        ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
            java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(responseMsg);
    }
}
