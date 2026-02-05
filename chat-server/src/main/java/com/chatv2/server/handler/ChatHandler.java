package com.chatv2.server.handler;

import com.chatv2.server.manager.ChatManager;
import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chat handler for processing chat-related requests.
 */
public class ChatHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private final ChatManager chatManager;
    private final MessageManager messageManager;
    private final SessionManager sessionManager;

    public ChatHandler(ChatManager chatManager, MessageManager messageManager, SessionManager sessionManager) {
        this.chatManager = chatManager;
        this.messageManager = messageManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        try {
            log.debug("Received chat message: {}", msg);

            if (msg.contains("CHAT_CREATE")) {
                // Handle chat creation
                String[] parts = msg.split(":");
                if (parts.length >= 5) {
                    String userIdStr = parts[1];
                    String chatType = parts[2];
                    String name = parts[3];
                    String description = parts[4];

                    java.util.UUID userId = java.util.UUID.fromString(userIdStr);
                    java.util.Set<java.util.UUID> members = java.util.Set.of(userId);

                    if ("GROUP".equals(chatType)) {
                        chatManager.createGroupChat(userId, name, description, members)
                            .thenAccept(chat -> {
                                ctx.writeAndFlush("CHAT_CREATE_RES:SUCCESS:" + chat.chatId());
                            })
                            .exceptionally(ex -> {
                                ctx.writeAndFlush("CHAT_CREATE_RES:ERROR:" + ex.getMessage());
                                return null;
                            });
                    } else {
                        chatManager.createPrivateChat(userId, userId)
                            .thenAccept(chat -> {
                                ctx.writeAndFlush("CHAT_CREATE_RES:SUCCESS:" + chat.chatId());
                            })
                            .exceptionally(ex -> {
                                ctx.writeAndFlush("CHAT_CREATE_RES:ERROR:" + ex.getMessage());
                                return null;
                            });
                    }
                }
            } else if (msg.contains("CHAT_LIST")) {
                // Handle chat list request
                String[] parts = msg.split(":");
                if (parts.length >= 2) {
                    String userIdStr = parts[1];
                    java.util.UUID userId = java.util.UUID.fromString(userIdStr);

                    chatManager.getUserChats(userId)
                        .thenAccept(chats -> {
                            StringBuilder sb = new StringBuilder("CHAT_LIST_RES:SUCCESS:");
                            for (var chat : chats) {
                                sb.append(chat.chatId()).append(",");
                            }
                            ctx.writeAndFlush(sb.toString());
                        })
                        .exceptionally(ex -> {
                            ctx.writeAndFlush("CHAT_LIST_RES:ERROR:" + ex.getMessage());
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            ctx.writeAndFlush("ERROR:" + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Chat handler exception", cause);
        ctx.writeAndFlush("ERROR:" + cause.getMessage());
    }
}
