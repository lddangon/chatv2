package com.chatv2.server.handler;

import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session handler for validating sessions.
 */
public class SessionHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(SessionHandler.class);
    private final SessionManager sessionManager;

    public SessionHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            log.debug("Received session message: {}", msg);

            if (msg.contains("SESSION_VALIDATE")) {
                // Handle session validation
                String[] parts = msg.split(":");
                if (parts.length >= 2) {
                    String token = parts[1];

                    sessionManager.validateSession(token)
                        .thenAccept(session -> {
                            ctx.writeAndFlush("SESSION_VALIDATE_RES:SUCCESS:" + session.userId());
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("SESSION_VALIDATE_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing session message", e);
            ctx.writeAndFlush("ERROR:" + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Session handler exception", cause);
        ctx.writeAndFlush("ERROR:" + cause.getMessage());
    }
}
