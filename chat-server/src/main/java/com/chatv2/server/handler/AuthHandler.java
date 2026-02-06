package com.chatv2.server.handler;

import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.manager.SessionManager;
import com.chatv2.server.manager.UserManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Authentication handler for processing login/register requests.
 * Works with ChatMessage instead of String.
 */
public class AuthHandler extends SimpleChannelInboundHandler<ChatMessage> {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private final UserManager userManager;
    private final SessionManager sessionManager;

    public AuthHandler(UserManager userManager, SessionManager sessionManager) {
        this.userManager = userManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            log.debug("Received auth message: type={}, messageId={}", msg.getMessageType(), msg.getMessageId());

            // Convert payload to string (temporary - will use proper protocol parsing)
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);

            // Parse message (simplified - in real implementation would use proper protocol)
            if (payload.contains("AUTH_LOGIN")) {
                // Handle login
                String[] parts = payload.split(":");
                if (parts.length >= 3) {
                    String username = parts[1];
                    String password = parts[2];

                    userManager.login(username, password)
                        .thenAccept(profile -> {
                            // Create session
                            sessionManager.createSession(profile.userId(), "unknown")
                                .thenAccept(session -> {
                                    String response = "AUTH_LOGIN_RES:SUCCESS:" + profile.userId() + ":" + session.token();
                                    ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_LOGIN_RES, (byte) 0x00,
                                        java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                                    ctx.writeAndFlush(responseMsg);
                                })
                                .exceptionally(ex -> {
                                    String response = "AUTH_LOGIN_RES:ERROR:" + ex.getMessage();
                                    ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_LOGIN_RES, (byte) 0x00,
                                        java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                                    ctx.writeAndFlush(responseMsg);
                                    return null;
                                });
                        })
                        .exceptionally(ex -> {
                            String response = "AUTH_LOGIN_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_LOGIN_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            } else if (payload.contains("AUTH_REGISTER")) {
                // Handle registration
                String[] parts = payload.split(":");
                if (parts.length >= 4) {
                    String username = parts[1];
                    String password = parts[2];
                    String fullName = parts[3];

                    userManager.register(username, password, fullName, null)
                        .thenAccept(profile -> {
                            String response = "AUTH_REGISTER_RES:SUCCESS:" + profile.userId();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_REGISTER_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                        })
                        .exceptionally(ex -> {
                            String response = "AUTH_REGISTER_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_REGISTER_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            } else if (payload.contains("AUTH_LOGOUT")) {
                // Handle logout
                String[] parts = payload.split(":");
                if (parts.length >= 2) {
                    String token = parts[1];

                    sessionManager.terminateSession(token)
                        .thenAccept(v -> {
                            String response = "AUTH_LOGOUT_RES:SUCCESS";
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_LOGOUT_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                        })
                        .exceptionally(ex -> {
                            String response = "AUTH_LOGOUT_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.AUTH_LOGOUT_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing auth message", e);
            String response = "ERROR:" + e.getMessage();
            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(responseMsg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Auth handler exception", cause);
        String response = "ERROR:" + cause.getMessage();
        ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
            java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(responseMsg);
    }
}
