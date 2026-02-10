package com.chatv2.server.handler;

import com.chatv2.common.model.Chat;
import com.chatv2.common.model.ChatType;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.MessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.handler.dto.ChatCreateRequest;
import com.chatv2.server.handler.dto.ChatListRequest;
import com.chatv2.server.manager.ChatManager;
import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Chat handler for processing chat-related requests.
 * Uses JSON payloads encoded/decoded via MessageCodec.
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

            if (msg.getMessageType() == ProtocolMessageType.CHAT_CREATE_REQ) {
                handleChatCreate(ctx, msg);
            } else if (msg.getMessageType() == ProtocolMessageType.CHAT_LIST_REQ) {
                handleChatList(ctx, msg);
            } else {
                log.warn("Unsupported message type: {}", msg.getMessageType());
            }
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            sendErrorResponse(ctx, "Processing error: " + e.getMessage(), msg.getMessageId());
        }
    }

    /**
     * Handles CHAT_CREATE_REQ (0x0400) request.
     * Expects ChatCreateRequest JSON with fields:
     * - chatType: "GROUP" or "PRIVATE"
     * - name: chat name (required for GROUP)
     * - description: chat description
     * - ownerId: creator UUID
     * - memberIds: array of member UUIDs
     * Sends CHAT_CREATE_RES (0x0401) with Chat object.
     */
    private void handleChatCreate(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Decode request payload
            ChatCreateRequest request = MessageCodec.decodeFromBytes(msg.getPayload(), ChatCreateRequest.class);
            request.validate();

            ChatType chatType = ChatType.valueOf(request.chatType().toUpperCase());

            log.debug("Creating {} chat with owner: {}", chatType, request.ownerId());

            if (chatType == ChatType.GROUP) {
                // Create group chat
                java.util.Set<java.util.UUID> members = request.memberIds() != null
                    ? request.memberIds()
                    : java.util.Set.of(request.ownerId());

                chatManager.createGroupChat(
                        request.ownerId(),
                        request.name(),
                        request.description(),
                        members
                    )
                    .thenAccept(chat -> sendChatResponse(ctx, chat, msg.getMessageId()))
                    .exceptionally(ex -> {
                        log.error("Error creating group chat", ex);
                        sendErrorResponse(ctx, ex.getMessage(), msg.getMessageId());
                        return null;
                    });
            } else {
                // Create private chat
                chatManager.createPrivateChat(request.ownerId(), request.ownerId())
                    .thenAccept(chat -> sendChatResponse(ctx, chat, msg.getMessageId()))
                    .exceptionally(ex -> {
                        log.error("Error creating private chat", ex);
                        sendErrorResponse(ctx, ex.getMessage(), msg.getMessageId());
                        return null;
                    });
            }
        } catch (Exception e) {
            log.error("Error parsing chat create request", e);
            sendErrorResponse(ctx, "Invalid request payload: " + e.getMessage(), msg.getMessageId());
        }
    }

    /**
     * Handles CHAT_LIST_REQ (0x0402) request.
     * Expects ChatListRequest JSON: {"userId": "uuid"}
     * Sends CHAT_LIST_RES (0x0403) with Chat[] array.
     */
    private void handleChatList(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Decode request payload
            ChatListRequest request = MessageCodec.decodeFromBytes(msg.getPayload(), ChatListRequest.class);
            request.validate();

            log.debug("Fetching chat list for user: {}", request.userId());

            chatManager.getUserChats(request.userId())
                .thenAccept(chats -> {
                    try {
                        // Send response with Chat[] array
                        byte[] responsePayload = MessageCodec.encodeToBytes(chats);
                        ChatMessage responseMsg = new ChatMessage(
                            ProtocolMessageType.CHAT_LIST_RES,
                            (byte) 0x00,
                            msg.getMessageId(),
                            System.currentTimeMillis(),
                            responsePayload
                        );
                        ctx.writeAndFlush(responseMsg);
                        log.debug("Chat list sent for user: {}, count: {}", request.userId(), chats.size());
                    } catch (Exception e) {
                        log.error("Error encoding chat list response", e);
                        sendErrorResponse(ctx, "Response encoding error", msg.getMessageId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error fetching chat list", ex);
                    sendErrorResponse(ctx, ex.getMessage(), msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error parsing chat list request", e);
            sendErrorResponse(ctx, "Invalid request payload: " + e.getMessage(), msg.getMessageId());
        }
    }

    /**
     * Sends a chat response message with Chat object.
     *
     * @param ctx channel context
     * @param chat chat object to send
     * @param originalMessageId message ID from the original request (for client request-response matching)
     */
    private void sendChatResponse(ChannelHandlerContext ctx, Chat chat, UUID originalMessageId) {
        try {
            byte[] responsePayload = MessageCodec.encodeToBytes(chat);
            ChatMessage responseMsg = new ChatMessage(
                ProtocolMessageType.CHAT_CREATE_RES,
                (byte) 0x00,
                originalMessageId,
                System.currentTimeMillis(),
                responsePayload
            );
            ctx.writeAndFlush(responseMsg);
            log.debug("Chat created successfully: {}", chat.chatId());
        } catch (Exception e) {
            log.error("Error encoding chat response", e);
            sendErrorResponse(ctx, "Response encoding error", originalMessageId);
        }
    }

    /**
     * Sends an error response message.
     *
     * @param ctx channel context
     * @param errorMessage error message text
     * @param originalMessageId message ID from the original request (for client request-response matching)
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage, UUID originalMessageId) {
        try {
            ChatMessage responseMsg = new ChatMessage(
                ProtocolMessageType.ERROR,
                (byte) 0x00,
                originalMessageId,
                System.currentTimeMillis(),
                errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            ctx.writeAndFlush(responseMsg);
        } catch (Exception e) {
            log.error("Error sending error response", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Chat handler exception", cause);
        sendErrorResponse(ctx, cause.getMessage(), null);
    }
}
