package com.chatv2.server.handler;

import com.chatv2.common.model.Message;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.MessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.handler.dto.MessageHistoryRequest;
import com.chatv2.server.manager.MessageManager;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Message handler for processing message requests.
 * Uses JSON payloads encoded/decoded via MessageCodec.
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

            if (msg.getMessageType() == ProtocolMessageType.MESSAGE_SEND_REQ) {
                handleMessageSend(ctx, msg);
            } else if (msg.getMessageType() == ProtocolMessageType.MESSAGE_HISTORY_REQ) {
                handleMessageHistory(ctx, msg);
            } else {
                log.warn("Unsupported message type: {}", msg.getMessageType());
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            sendErrorResponse(ctx, "Processing error: " + e.getMessage(), msg.getMessageId());
        }
    }

    /**
     * Handles MESSAGE_SEND_REQ (0x0500) request.
     * Expects Message object in JSON format as payload.
     * Sends MESSAGE_SEND_RES (0x0501) response.
     */
    private void handleMessageSend(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Decode Message from JSON payload
            Message messageRequest = MessageCodec.decodeFromBytes(msg.getPayload(), Message.class);

            log.debug("Sending message to chat: {}", messageRequest.chatId());

            messageManager.sendMessage(
                    messageRequest.chatId(),
                    messageRequest.senderId(),
                    messageRequest.content(),
                    messageRequest.messageType().name(),
                    messageRequest.replyToMessageId()
                )
                .thenAccept(message -> {
                    try {
                        // Send response with Message object
                        byte[] responsePayload = MessageCodec.encodeToBytes(message);
                        ChatMessage responseMsg = new ChatMessage(
                            ProtocolMessageType.MESSAGE_SEND_RES,
                            (byte) 0x00,
                            msg.getMessageId(),
                            System.currentTimeMillis(),
                            responsePayload
                        );
                        ctx.writeAndFlush(responseMsg);
                        log.debug("Message sent successfully: {}", message.messageId());
                    } catch (Exception e) {
                        log.error("Error encoding message response", e);
                        sendErrorResponse(ctx, "Response encoding error", msg.getMessageId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error sending message", ex);
                    sendErrorResponse(ctx, ex.getMessage(), msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error parsing message send request", e);
            sendErrorResponse(ctx, "Invalid request payload: " + e.getMessage(), msg.getMessageId());
        }
    }

    /**
     * Handles MESSAGE_HISTORY_REQ (0x0503) request.
     * Expects MessageHistoryRequest JSON: {"chatId": "uuid", "limit": N}
     * Sends MESSAGE_HISTORY_RES (0x0504) with Message[] array.
     */
    private void handleMessageHistory(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Decode request payload
            MessageHistoryRequest request = MessageCodec.decodeFromBytes(msg.getPayload(), MessageHistoryRequest.class);
            request.validate();

            log.debug("Fetching message history for chat: {}, limit: {}", request.chatId(), request.limit());

            messageManager.getMessageHistory(request.chatId(), request.limit(), 0, null)
                .thenAccept(messages -> {
                    try {
                        // Send response with Message[] array
                        byte[] responsePayload = MessageCodec.encodeToBytes(messages);
                        ChatMessage responseMsg = new ChatMessage(
                            ProtocolMessageType.MESSAGE_HISTORY_RES,
                            (byte) 0x00,
                            msg.getMessageId(),
                            System.currentTimeMillis(),
                            responsePayload
                        );
                        ctx.writeAndFlush(responseMsg);
                        log.debug("Message history sent for chat: {}, count: {}", request.chatId(), messages.size());
                    } catch (Exception e) {
                        log.error("Error encoding message history response", e);
                        sendErrorResponse(ctx, "Response encoding error", msg.getMessageId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error fetching message history", ex);
                    sendErrorResponse(ctx, ex.getMessage(), msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error parsing message history request", e);
            sendErrorResponse(ctx, "Invalid request payload: " + e.getMessage(), msg.getMessageId());
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
        log.error("Message handler exception", cause);
        sendErrorResponse(ctx, cause.getMessage(), null);
    }
}
