package com.chatv2.server.handler;

import com.chatv2.common.model.Session;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.MessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.handler.dto.SessionValidateRequest;
import com.chatv2.server.manager.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Session handler for validating sessions.
 * Uses JSON payloads encoded/decoded via MessageCodec.
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

            if (msg.getMessageType() == ProtocolMessageType.SESSION_VALIDATE_REQ) {
                handleSessionValidate(ctx, msg);
            } else {
                log.warn("Unsupported message type: {}", msg.getMessageType());
            }
        } catch (Exception e) {
            log.error("Error processing session message", e);
            sendErrorResponse(ctx, "Processing error: " + e.getMessage(), msg.getMessageId());
        }
    }

    /**
     * Handles SESSION_VALIDATE_REQ (0x0200) request.
     * Expects SessionValidateRequest JSON: {"token": "xxx"}
     * Sends SESSION_VALIDATE_RES (0x0201) with Session object.
     */
    private void handleSessionValidate(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Decode request payload
            SessionValidateRequest request = MessageCodec.decodeFromBytes(msg.getPayload(), SessionValidateRequest.class);
            request.validate();

            log.debug("Validating session with token: {}", request.token());

            sessionManager.validateSession(request.token())
                .thenAccept(session -> {
                    try {
                        // Send response with Session object
                        byte[] responsePayload = MessageCodec.encodeToBytes(session);
                        ChatMessage responseMsg = new ChatMessage(
                            ProtocolMessageType.SESSION_VALIDATE_RES,
                            (byte) 0x00,
                            msg.getMessageId(),
                            System.currentTimeMillis(),
                            responsePayload
                        );
                        ctx.writeAndFlush(responseMsg);
                        log.debug("Session validated successfully for user: {}", session.userId());
                    } catch (Exception e) {
                        log.error("Error encoding session response", e);
                        sendErrorResponse(ctx, "Response encoding error", msg.getMessageId());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error validating session", ex);
                    sendErrorResponse(ctx, ex.getMessage(), msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error parsing session validate request", e);
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
        log.error("Session handler exception", cause);
        sendErrorResponse(ctx, cause.getMessage(), null);
    }
}
