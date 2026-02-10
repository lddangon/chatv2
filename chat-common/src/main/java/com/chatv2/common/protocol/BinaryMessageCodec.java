package com.chatv2.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Binary codec for ChatV2 protocol according to PROTOCOL_SPEC.md.
 * This is a Netty MessageToMessageCodec that encodes/decodes protocol messages.
 *
 * Packet Format:
 * [PacketHeader: 40 bytes][Payload: N bytes]
 * Total: 40 + N bytes
 * Header structure:
 *   [0-3]   Magic Number (4 bytes)
 *   [4-5]   Message Type (2 bytes)
 *   [6]     Version (1 byte)
 *   [7]     Flags (1 byte)
 *   [8-23]  Message ID - Full UUID (16 bytes)
 *   [24-27] Payload Length (4 bytes)
 *   [28-35] Timestamp (8 bytes)
 *   [36-39] Checksum (4 bytes)
 *
 * Encoding: ChatMessage -> ByteBuf
 * Decoding: ByteBuf -> ChatMessage
 *
 * This handler is marked as sharable since it has no internal state.
 */
@ChannelHandler.Sharable
public class BinaryMessageCodec extends MessageToMessageCodec<ByteBuf, ChatMessage> {
    private static final Logger log = LoggerFactory.getLogger(BinaryMessageCodec.class);

    // Maximum payload size to prevent OOM attacks
    private static final int MAX_PAYLOAD_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    protected void encode(ChannelHandlerContext ctx, ChatMessage msg, List<Object> out) throws Exception {
        try {
            // Use ChatMessage.encode() directly to avoid format mismatch
            byte[] messageBytes = msg.encode();
            out.add(ctx.alloc().buffer(messageBytes.length).writeBytes(messageBytes));
            log.debug("Encoded message: type={}, payloadLength={}, totalLength={}",
                msg.getMessageType(), msg.getPayload().length, messageBytes.length);
        } catch (Exception e) {
            log.error("Failed to encode message", e);
            throw e;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        try {
            // Use ChatMessage.decode() directly to avoid format mismatch
            // Check minimum length for header
            if (buf.readableBytes() < ChatMessage.HEADER_SIZE) {
                return; // Wait for more data
            }

            // Mark reader index for rollback on error
            buf.markReaderIndex();

            // Read header to determine total message length
            int magicNumber = buf.getInt(buf.readerIndex());
            if (magicNumber != ChatMessage.MAGIC_NUMBER) {
                buf.resetReaderIndex();
                throw new IllegalStateException("Invalid magic number: 0x" + Integer.toHexString(magicNumber));
            }

            // Read payload length
            int payloadLength = buf.getInt(buf.readerIndex() + 24); // Offset to payload length (after 16-byte UUID)

            // Check payload length to prevent OOM attacks
            if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_SIZE) {
                buf.resetReaderIndex();
                throw new IllegalStateException(
                    "Invalid payload length: " + payloadLength + " (max: " + MAX_PAYLOAD_SIZE + ")"
                );
            }

            int totalLength = ChatMessage.HEADER_SIZE + payloadLength;

            // Check if we have enough bytes
            if (buf.readableBytes() < totalLength) {
                buf.resetReaderIndex();
                return; // Wait for more data
            }

            // Read the complete message
            byte[] messageBytes = new byte[totalLength];
            buf.readBytes(messageBytes);
            
            // Decode the message
            ChatMessage message = ChatMessage.decode(messageBytes);
            out.add(message);
            log.debug("Decoded message: type={}, payloadLength={}", message.getMessageType(), message.getPayload().length);

        } catch (Exception e) {
            log.error("Failed to decode message", e);
            buf.resetReaderIndex();
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in BinaryMessageCodec", cause);
        // Forward the exception to the next handler in the pipeline
        ctx.fireExceptionCaught(cause);
    }

    /**
     * Deserializes JSON payload to the appropriate class based on message type.
     * This helper method is used by message handlers after decoding the binary packet.
     *
     * @param messageType the protocol message type
     * @param jsonPayload the JSON string payload
     * @return deserialized object or null if deserialization fails
     */
    public static Object deserializePayload(ProtocolMessageType messageType, String jsonPayload) {
        if (jsonPayload == null || jsonPayload.isEmpty()) {
            return null;
        }

        Class<?> payloadClass = getPayloadClass(messageType);
        if (payloadClass == Object.class) {
            // Return raw JSON as string if no specific class is known
            return jsonPayload;
        }

        try {
            return MessageCodec.decode(jsonPayload, payloadClass);
        } catch (IOException e) {
            log.warn("Failed to deserialize payload for message type {}: {}", messageType, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the expected payload class for a given message type.
     * Used for JSON deserialization.
     *
     * @param messageType protocol message type
     * @return payload class
     */
    private static Class<?> getPayloadClass(ProtocolMessageType messageType) {
        return switch (messageType) {
            // Authentication messages
            case AUTH_REGISTER_REQ, AUTH_REGISTER_RES -> com.chatv2.common.model.UserProfile.class;
            case AUTH_LOGIN_REQ, AUTH_LOGIN_RES -> com.chatv2.common.model.Session.class;
            
            // Chat management
            case CHAT_CREATE_REQ, CHAT_CREATE_RES -> com.chatv2.common.model.Chat.class;
            
            // Messaging
            case MESSAGE_SEND_REQ, MESSAGE_SEND_RES, MESSAGE_RECEIVE -> com.chatv2.common.model.Message.class;
            
            // Default: return raw bytes (Object.class)
            default -> Object.class;
        };
    }
}
