package com.chatv2.server.handler;

import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Message handler for processing message requests.
 * Works with ChatMessage instead of String.
 */
public class MessageHandler extends SimpleChannelInboundHandler<ChatMessage> {
    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);
    private final MessageManager messageManager;
    private final SessionManager sessionManager;

    public MessageHandler(MessageManager messageManager, SessionManager sessionManager) {
        this.messageManager = messageManager;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            log.debug("Received message: type={}, messageId={}", msg.getMessageType(), msg.getMessageId());

            // Convert payload to string (temporary - will use proper protocol parsing)
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);

            if (payload.contains("MESSAGE_SEND")) {
                // Handle message sending
                String[] parts = payload.split(":");
                if (parts.length >= 4) {
                    String chatIdStr = parts[1];
                    String senderIdStr = parts[2];
                    String content = parts[3];

                    java.util.UUID chatId = java.util.UUID.fromString(chatIdStr);
                    java.util.UUID senderId = java.util.UUID.fromString(senderIdStr);

                    messageManager.sendMessage(chatId, senderId, content, "TEXT", null)
                        .thenAccept(message -> {
                            String response = "MESSAGE_SEND_RES:SUCCESS:" + message.messageId();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                        })
                        .exceptionally(ex -> {
                            String response = "MESSAGE_SEND_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.MESSAGE_SEND_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            } else if (payload.contains("MESSAGE_HISTORY")) {
                // Handle message history request
                String[] parts = payload.split(":");
                if (parts.length >= 3) {
                    String chatIdStr = parts[1];
                    int limit = Integer.parseInt(parts[2]);

                    java.util.UUID chatId = java.util.UUID.fromString(chatIdStr);

                    messageManager.getMessageHistory(chatId, limit, 0, null)
                        .thenAccept(messages -> {
                            StringBuilder sb = new StringBuilder("MESSAGE_HISTORY_RES:SUCCESS:");
                            for (var message : messages) {
                                sb.append(message.messageId()).append(",");
                            }
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.MESSAGE_HISTORY_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), sb.toString().getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                        })
                        .exceptionally(ex -> {
                            String response = "MESSAGE_HISTORY_RES:ERROR:" + ex.getMessage();
                            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.MESSAGE_HISTORY_RES, (byte) 0x00,
                                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
                            ctx.writeAndFlush(responseMsg);
                            return null;
                        });
                }
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            String response = "ERROR:" + e.getMessage();
            ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
                java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(responseMsg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Message handler exception", cause);
        String response = "ERROR:" + cause.getMessage();
        ChatMessage responseMsg = new ChatMessage(ProtocolMessageType.ERROR, (byte) 0x00,
            java.util.UUID.randomUUID(), System.currentTimeMillis(), response.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(responseMsg);
    }
}
