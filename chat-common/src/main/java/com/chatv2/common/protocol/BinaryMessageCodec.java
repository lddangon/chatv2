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
 * [PacketHeader: 28 bytes][Payload: N bytes][Checksum: 4 bytes]
 * Total: 28 + N + 4 bytes
 *
 * Encoding: ChatMessage -> ByteBuf
 * Decoding: ByteBuf -> ChatMessage
 *
 * This handler is marked as sharable since it has no internal state.
 */
@ChannelHandler.Sharable
public class BinaryMessageCodec extends MessageToMessageCodec<ByteBuf, ChatMessage> {
    private static final Logger log = LoggerFactory.getLogger(BinaryMessageCodec.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ChatMessage msg, List<Object> out) throws Exception {
        try {
            // 1. Serialize payload to JSON using MessageCodec
            byte[] payloadBytes;
            Object payload = msg.getPayload();
            if (payload instanceof byte[]) {
                // Payload is already bytes (e.g., encrypted data)
                payloadBytes = (byte[]) payload;
            } else if (payload != null) {
                // Serialize object to JSON
                String jsonPayload = MessageCodec.encode(payload);
                payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
            } else {
                payloadBytes = new byte[0];
            }

            // 2. Validate payload size on encoding
            if (payloadBytes.length > ProtocolConstants.MAX_PAYLOAD_SIZE) {
                log.error("Payload too large for encoding: size={}, max={}", 
                    payloadBytes.length, ProtocolConstants.MAX_PAYLOAD_SIZE);
                throw new IllegalArgumentException("Payload size exceeds maximum allowed");
            }

            // 3. Create header
            short messageTypeCode = msg.getMessageType().getCode();
            byte flags = msg.getFlags();
            long timestamp = msg.getTimestamp();

            PacketHeader header = new PacketHeader(
                PacketHeader.MAGIC,
                messageTypeCode,
                PacketHeader.VERSION,
                flags,
                PacketHeader.toCompactUuid(msg.getMessageId()),
                payloadBytes.length,
                timestamp
            );

            // 4. Allocate buffer (header + payload + checksum)
            int totalLength = PacketHeader.SIZE + payloadBytes.length + 4; // +4 for CRC32
            ByteBuf buffer = ctx.alloc().buffer(totalLength);

            // 5. Write header
            ByteBuffer headerBuffer = ByteBuffer.allocate(PacketHeader.SIZE);
            header.write(headerBuffer);
            buffer.writeBytes(headerBuffer.array());

            // 6. Write payload
            buffer.writeBytes(payloadBytes);

            // 7. Calculate and write CRC32 checksum
            CRC32 crc32 = new CRC32();
            byte[] packetData = new byte[PacketHeader.SIZE + payloadBytes.length];
            buffer.getBytes(0, packetData);
            crc32.update(packetData);
            buffer.writeInt((int) crc32.getValue());

            out.add(buffer);
            log.debug("Encoded message: type={}, payloadLength={}, totalLength={}",
                msg.getMessageType(), payloadBytes.length, totalLength);

        } catch (Exception e) {
            log.error("Failed to encode message", e);
            throw e;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        try {
            // 1. Check minimum length for header
            if (buf.readableBytes() < PacketHeader.SIZE) {
                log.debug("Not enough bytes for header: {}", buf.readableBytes());
                return; // Wait for more data
            }

            // 2. Mark reader index for rollback on error
            buf.markReaderIndex();

            // 3. Read header
            ByteBuffer headerBuffer = ByteBuffer.allocate(PacketHeader.SIZE);
            buf.readBytes(headerBuffer.array());
            PacketHeader header = PacketHeader.read(headerBuffer);

            // 4. Validate magic number
            if (header.magic() != PacketHeader.MAGIC) {
                log.error("Invalid magic number: 0x{}", String.format("%08X", header.magic()));
                buf.resetReaderIndex();
                throw new IllegalStateException("Invalid packet header magic number");
            }

            // 5. Validate payload size to prevent DoS attacks
            if (header.payloadLength() < 0) {
                String remoteAddress = ctx.channel().remoteAddress().toString();
                log.warn("SECURITY ALERT: Negative payload length from {}: payloadLength={}",
                    remoteAddress, header.payloadLength());
                buf.resetReaderIndex();
                throw new IllegalStateException("Negative payload length");
            }

            if (header.payloadLength() > ProtocolConstants.MAX_PAYLOAD_SIZE) {
                String remoteAddress = ctx.channel().remoteAddress().toString();
                log.warn("SECURITY ALERT: Potential DoS attack from {}: payloadLength={}, max={}",
                    remoteAddress, header.payloadLength(), ProtocolConstants.MAX_PAYLOAD_SIZE);
                buf.resetReaderIndex();
                throw new IllegalStateException("Payload size exceeds maximum allowed");
            }

            // 7. Check if we have enough bytes for payload + checksum
            int requiredLength = header.payloadLength() + 4; // +4 for CRC32
            if (buf.readableBytes() < requiredLength) {
                log.debug("Not enough bytes for payload + checksum: available={}, required={}",
                    buf.readableBytes(), requiredLength);
                buf.resetReaderIndex();
                return; // Wait for more data
            }

            // 8. Read payload
            byte[] payloadBytes = new byte[header.payloadLength()];
            buf.readBytes(payloadBytes);

            // 9. Read and validate CRC32 checksum
            int receivedChecksum = buf.readInt();
            CRC32 crc32 = new CRC32();
            crc32.update(headerBuffer.array());
            crc32.update(payloadBytes);
            int calculatedChecksum = (int) crc32.getValue();

            if (receivedChecksum != calculatedChecksum) {
                log.error("CRC32 checksum mismatch: received={}, calculated={}",
                    receivedChecksum, calculatedChecksum);
                throw new IllegalStateException("CRC32 checksum mismatch");
            }

            // 10. For encrypted or empty payload, keep as bytes
            // For unencrypted payload, deserialize from JSON
            byte[] finalPayloadBytes = payloadBytes;
            ChatMessage message = new ChatMessage(
                ProtocolMessageType.fromCode(header.messageType()),
                header.flags(),
                header.toFullUuid(),
                header.timestamp(),
                finalPayloadBytes.length > 0 ? finalPayloadBytes : null
            );

            out.add(message);
            log.debug("Decoded message: type={}, payloadLength={}", header.messageType(), payloadBytes.length);

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
