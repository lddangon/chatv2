package com.chatv2.server.handler;

import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message handler for processing message requests.
 */
public class MessageHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    private final MessageManager messageManager;
    private final SessionManager sessionManager;

    public MessageHandler(MessageManager messageManager, SessionManager sessionManager) {
        this.messageManager = messageManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String request) {
        try {
            log.debug("Received message: {}", request);

            if (request.contains("MESSAGE_SEND")) {
                // Handle message sending
                String[] parts = request.split(":");
                if (parts.length >= 4) {
                    String chatIdStr = parts[1];
                    String senderIdStr = parts[2];
                    String content = parts[3];

                    java.util.UUID chatId = java.util.UUID.fromString(chatIdStr);
                    java.util.UUID senderId = java.util.UUID.fromString(senderIdStr);

                    messageManager.sendMessage(chatId, senderId, content, "TEXT", null)
                        .thenAccept(message -> {
                            ctx.writeAndFlush("MESSAGE_SEND_RES:SUCCESS:" + message.messageId());
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("MESSAGE_SEND_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            } else if (request.contains("MESSAGE_HISTORY")) {
                // Handle message history request
                String[] parts = request.split(":");
                if (parts.length >= 3) {
                    String chatIdStr = parts[1];
                    int limit = Integer.parseInt(parts[2]);

                    java.util.UUID chatId = java.util.UUID.fromString(chatIdStr);

                    messageManager.getMessageHistory(chatId, limit, 0, null)
                        .thenAccept(messages -> {
                            StringBuilder sb = new StringBuilder("MESSAGE_HISTORY_RES:SUCCESS:");
                            for (var msg : messages) {
                                sb.append(msg.messageId()).append(",");
                            }
                            ctx.writeAndFlush(sb.toString());
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("MESSAGE_HISTORY_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            ctx.writeAndFlush("ERROR:" + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Message handler exception", cause);
        ctx.writeAndFlush("ERROR:" + cause.getMessage());
    }
}
