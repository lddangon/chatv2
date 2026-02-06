package com.chatv2.server.handler;

import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.manager.ChatManager;
import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Chat handler for processing chat-related requests.
 * Works with ChatMessage instead of String.
 */
public class ChatHandler extends SimpleChannelInboundHandler<ChatMessage> {
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
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            log.debug("Received chat message: type={}, messageId={}", msg.getMessageType(), msg.getMessageId());

            // Convert payload to string (temporary - will use proper protocol parsing)
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);

            if (payload.contains("CHAT_CREATE")) {
                // Handle chat creation
                String[] parts = payload.split(":");
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
                                String response = "CHAT_CREATE_RES:SUCCESS:" + chat.chatId();
                                ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.CHAT_CREATE_RES, (byte) 0x00,
                                    java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                                ctx.writeAndFlush(responseMsg);
                            })
                            .exceptionally(ex -> {
                                String response = "CHAT_CREATE_RES:ERROR:" + ex.getMessage();
                                ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.CHAT_CREATE_RES, (byte) 0x00,
                                    java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                                ctx.writeAndFlush(responseMsg);
                                return null;
                            });
                    } else {
                        chatManager.createPrivateChat(userId, userId)
                            .thenAccept(chat -> {
                                String response = "CHAT_CREATE_RES:SUCCESS:" + chat.chatId();
                                ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.CHAT_CREATE_RES, (byte) 0x00,
                                    java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                                ctx.writeAndFlush(responseMsg);
                            })
                            .exceptionally(ex -> {
                                String response = "CHAT_CREATE_RES:ERROR:" + ex.getMessage();
                                ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.CHAT_CREATE_RES, (byte) 0x00,
                                    java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                                ctx.writeAndFlush(responseMsg);
                                return null;
                            });
                    }
                }
            } else if (payload.contains("CHAT_LIST")) {
                // Handle chat list request
                String[] parts = payload.split(":");
                if (parts.length >= 2) {
                    String userIdStr = parts[1];
                    java.util.UUID userId = java.util.UUID.fromString(userIdStr);

                    chatManager.getUserChats(userId)
                        .thenAccept(chats -> {
                            StringBuilder sb = new StringBuilder("CHAT_LIST_RES:SUCCESS:");
                            for (var chat : chats) {
                                sb.append(chat.chatId()).append(",");
                            }
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.CHAT_LIST_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), sb.toString().getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                        })
                        .exceptionally(ex -> {
                            String response = "CHAT_LIST_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.CHAT_LIST_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            String response = "ERROR:" + e.getMessage();
            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(responseMsg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Chat handler exception", cause);
        String response = "ERROR:" + cause.getMessage();
        ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
            java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(responseMsg);
    }
}
