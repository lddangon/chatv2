package com.chatv2.server.handler;

import com.chatv2.common.model.Session;
import com.chatv2.common.model.UserProfile;
import com.chatv2.common.protocol.ChatMessage;
import com.chatv2.common.protocol.MessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.chatv2.server.handler.dto.ErrorResponse;
import com.chatv2.server.handler.dto.LoginRequest;
import com.chatv2.server.handler.dto.LogoutRequest;
import com.chatv2.server.manager.SessionManager;
import com.chatv2.server.manager.UserManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Authentication handler for processing login/register/logout requests.
 * Handles JSON payloads through MessageCodec.
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

            // Determine the message type and handle accordingly
            switch (msg.getMessageType()) {
                case AUTH_LOGIN_REQ:
                    handleLogin(ctx, msg);
                    break;
                case AUTH_REGISTER_REQ:
                    handleRegister(ctx, msg);
                    break;
                case AUTH_LOGOUT_REQ:
                    handleLogout(ctx, msg);
                    break;
                default:
                    log.warn("Unknown auth message type: {}", msg.getMessageType());
                    sendErrorResponse(ctx, "Unknown auth message type: " + msg.getMessageType(), "INVALID_MESSAGE_TYPE", msg.getMessageId());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing auth message", e);
            sendErrorResponse(ctx, "Internal server error: " + e.getMessage(), "INTERNAL_ERROR", msg.getMessageId());
        }
    }

    /**
     * Handles login request with JSON payload: {"username": "xxx", "password": "xxx"}
     * Sends AUTH_LOGIN_RES with Session object in JSON.
     *
     * @param ctx channel context
     * @param msg incoming message
     */
    private void handleLogin(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Deserialize login request from JSON
            LoginRequest request = MessageCodec.decodeFromBytes(msg.getPayload(), LoginRequest.class);
            request.validate();

            log.debug("Processing login request for user: {}", request.username());

            userManager.login(request.username(), request.password())
                .thenAccept(profile -> {
                    // Create session
                    String deviceInfo = ctx.channel().remoteAddress().toString();
                    sessionManager.createSession(profile.userId(), deviceInfo)
                        .thenAccept(session -> {
                            // Send success response with Session object
                            sendSuccessResponse(ctx, ProtocolMessageType.AUTH_LOGIN_RES, session, msg.getMessageId());
                            log.info("User {} logged in successfully", request.username());
                        })
                        .exceptionally(ex -> {
                            log.error("Failed to create session for user {}", request.username(), ex);
                            sendErrorResponse(ctx, "Failed to create session: " + ex.getMessage(), "SESSION_ERROR", msg.getMessageId());
                            return null;
                        });
                })
                .exceptionally(ex -> {
                    log.error("Login failed for user {}", request.username(), ex);
                    sendErrorResponse(ctx, "Login failed: " + ex.getMessage(), "LOGIN_FAILED", msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error processing login request", e);
            sendErrorResponse(ctx, "Invalid login request: " + e.getMessage(), "INVALID_REQUEST", msg.getMessageId());
        }
    }

    /**
     * Handles register request with UserProfile JSON payload.
     * Sends AUTH_REGISTER_RES with UserProfile object in JSON.
     *
     * @param ctx channel context
     * @param msg incoming message
     */
    private void handleRegister(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Deserialize UserProfile from JSON
            UserProfile profile = MessageCodec.decodeFromBytes(msg.getPayload(), UserProfile.class);

            log.debug("Processing registration request for user: {}", profile.username());

            userManager.register(profile)
                .thenAccept(createdProfile -> {
                    // Send success response with UserProfile object (safe profile without sensitive data)
                    UserProfile publicProfile = createdProfile.toPublicProfile();
                    sendSuccessResponse(ctx, ProtocolMessageType.AUTH_REGISTER_RES, publicProfile, msg.getMessageId());
                    log.info("User {} registered successfully", profile.username());
                })
                .exceptionally(ex -> {
                    log.error("Registration failed for user {}", profile.username(), ex);
                    sendErrorResponse(ctx, "Registration failed: " + ex.getMessage(), "REGISTRATION_FAILED", msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error processing registration request", e);
            sendErrorResponse(ctx, "Invalid registration request: " + e.getMessage(), "INVALID_REQUEST", msg.getMessageId());
        }
    }

    /**
     * Handles logout request with JSON payload: {"token": "xxx"}
     * Sends AUTH_LOGOUT_RES with success status.
     *
     * @param ctx channel context
     * @param msg incoming message
     */
    private void handleLogout(ChannelHandlerContext ctx, ChatMessage msg) {
        try {
            // Deserialize logout request from JSON
            LogoutRequest request = MessageCodec.decodeFromBytes(msg.getPayload(), LogoutRequest.class);
            request.validate();

            log.debug("Processing logout request for token: {}", request.token());

            sessionManager.terminateSession(request.token())
                .thenAccept(v -> {
                    // Send success response
                    sendSuccessResponse(ctx, ProtocolMessageType.AUTH_LOGOUT_RES, "{\"status\":\"logged_out\"}", msg.getMessageId());
                    log.info("Logout successful for token: {}", request.token());
                })
                .exceptionally(ex -> {
                    log.error("Logout failed for token {}", request.token(), ex);
                    sendErrorResponse(ctx, "Logout failed: " + ex.getMessage(), "LOGOUT_FAILED", msg.getMessageId());
                    return null;
                });
        } catch (Exception e) {
            log.error("Error processing logout request", e);
            sendErrorResponse(ctx, "Invalid logout request: " + e.getMessage(), "INVALID_REQUEST", msg.getMessageId());
        }
    }

    /**
     * Sends a success response with the given payload object serialized to JSON.
     *
     * @param ctx channel context
     * @param messageType response message type
     * @param payload payload object to serialize
     * @param originalMessageId message ID from the original request (for client request-response matching)
     */
    private void sendSuccessResponse(ChannelHandlerContext ctx, ProtocolMessageType messageType, Object payload, UUID originalMessageId) {
        try {
            byte[] payloadBytes = MessageCodec.encodeToBytes(payload);
            ChatMessage responseMsg = new ChatMessage(
                messageType,
                (byte) 0x00,
                originalMessageId,
                System.currentTimeMillis(),
                payloadBytes
            );
            ctx.writeAndFlush(responseMsg);
        } catch (Exception e) {
            log.error("Failed to send success response", e);
            sendErrorResponse(ctx, "Failed to send response: " + e.getMessage(), "INTERNAL_ERROR", originalMessageId);
        }
    }

    /**
     * Sends an error response with the given error message.
     *
     * @param ctx channel context
     * @param error error message
     * @param code error code
     * @param originalMessageId message ID from the original request (for client request-response matching)
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String error, String code, UUID originalMessageId) {
        try {
            ErrorResponse errorResponse = ErrorResponse.create(error, code);
            byte[] payloadBytes = MessageCodec.encodeToBytes(errorResponse);
            ChatMessage responseMsg = new ChatMessage(
                ProtocolMessageType.ERROR,
                (byte) 0x00,
                originalMessageId,
                System.currentTimeMillis(),
                payloadBytes
            );
            ctx.writeAndFlush(responseMsg);
        } catch (Exception e) {
            log.error("Failed to send error response", e);
            // Last resort: send plain text error
            String plainError = "{\"error\":\"" + error + "\",\"code\":\"" + code + "\"}";
            ChatMessage responseMsg = new ChatMessage(
                ProtocolMessageType.ERROR,
                (byte) 0x00,
                originalMessageId,
                System.currentTimeMillis(),
                plainError.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            ctx.writeAndFlush(responseMsg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Auth handler exception", cause);
        sendErrorResponse(ctx, "Handler error: " + cause.getMessage(), "HANDLER_ERROR", null);
    }
}
