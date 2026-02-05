package com.chatv2.server.handler;

import com.chatv2.server.manager.SessionManager;
import com.chatv2.server.manager.UserManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication handler for processing login/register requests.
 */
public class AuthHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private final UserManager userManager;
    private final SessionManager sessionManager;

    public AuthHandler(UserManager userManager, SessionManager sessionManager) {
        this.userManager = userManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            log.debug("Received auth message: {}", msg);

            // Parse message (simplified - in real implementation would use proper protocol)
            if (msg.contains("AUTH_LOGIN")) {
                // Handle login
                String[] parts = msg.split(":");
                if (parts.length >= 3) {
                    String username = parts[1];
                    String password = parts[2];

                    userManager.login(username, password)
                        .thenAccept(profile -> {
                            // Create session
                            sessionManager.createSession(profile.userId(), "unknown")
                                .thenAccept(session -> {
                                    ctx.writeAndFlush("AUTH_LOGIN_RES:SUCCESS:" + profile.userId() + ":" + session.token());
                                })
                                .exceptionally(ex -> {
                                    ctx.writeAndFlush("AUTH_LOGIN_RES:ERROR:" + ex.getMessage());
                                    return null;
                                });
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("AUTH_LOGIN_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            } else if (msg.contains("AUTH_REGISTER")) {
                // Handle registration
                String[] parts = msg.split(":");
                if (parts.length >= 4) {
                    String username = parts[1];
                    String password = parts[2];
                    String fullName = parts[3];

                    userManager.register(username, password, fullName, null)
                        .thenAccept(profile -> {
                            ctx.writeAndFlush("AUTH_REGISTER_RES:SUCCESS:" + profile.userId());
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("AUTH_REGISTER_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            } else if (msg.contains("AUTH_LOGOUT")) {
                // Handle logout
                String[] parts = msg.split(":");
                if (parts.length >= 2) {
                    String token = parts[1];

                    sessionManager.terminateSession(token)
                        .thenAccept(v -> {
                            ctx.writeAndFlush("AUTH_LOGOUT_RES:SUCCESS");
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("AUTH_LOGOUT_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing auth message", e);
            ctx.writeAndFlush("ERROR:" + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Auth handler exception", cause);
        ctx.writeAndFlush("ERROR:" + cause.getMessage());
    }
}
