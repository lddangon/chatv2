package com.chatv2.common.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BinaryMessageCodecDoSTest {

    private BinaryMessageCodec codec;
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        codec = new BinaryMessageCodec();
        channel = new EmbeddedChannel(codec);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.finish();
        }
    }

    @Test
    @DisplayName("Should reject messages with payload size exceeding MAX_PAYLOAD_SIZE")
    void testRejectOversizedPayload() {
        // Given - Create a message with payload size exceeding MAX_PAYLOAD_SIZE
        int oversizePayloadLength = ProtocolConstants.MAX_PAYLOAD_SIZE + 1;
        
        // Create a valid header but with oversized payload length
        PacketHeader header = new PacketHeader(
            PacketHeader.MAGIC,
            ProtocolMessageType.MESSAGE_SEND_REQ.getCode(),
            PacketHeader.VERSION,
            (byte) 0,
            PacketHeader.toCompactUuid(UUID.randomUUID()),
            oversizePayloadLength,
            System.currentTimeMillis()
        );
        
        // Create buffer with header + dummy payload + checksum
        ByteBuffer headerBuffer = ByteBuffer.allocate(PacketHeader.SIZE);
        header.write(headerBuffer);
        
        ByteBuf buffer = Unpooled.buffer(PacketHeader.SIZE + oversizePayloadLength + 4);
        buffer.writeBytes(headerBuffer.array());
        
        // Write dummy payload (just zeros)
        buffer.writeZero(oversizePayloadLength);
        
        // Write dummy checksum
        buffer.writeInt(0);
        
        // When - Try to decode the message
        // Expect an exception to be thrown
        DecoderException exception = assertThrows(DecoderException.class, () -> {
            channel.writeInbound(buffer);
        });
        
        // Then - Exception should have the correct message
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Payload size exceeds maximum allowed");
        
        // Handler should still be in pipeline
        assertThat(channel.pipeline().context(BinaryMessageCodec.class)).isNotNull();
    }
    
    @Test
    @DisplayName("Should accept messages with payload size less than MAX_PAYLOAD_SIZE")
    void testAcceptNormalSizePayload() throws Exception {
        // Given - Create a normal message with payload size less than MAX_PAYLOAD_SIZE
        byte[] payloadBytes = "Test message for DoS protection".getBytes();
        
        ChatMessage originalMessage = new ChatMessage(
            ProtocolMessageType.MESSAGE_SEND_REQ,
            (byte) 0,
            UUID.randomUUID(),
            System.currentTimeMillis(),
            payloadBytes
        );

        // When - Encode message using channel
        channel.writeOutbound(originalMessage);
        ByteBuf encodedMessage = channel.readOutbound();
        
        // And decode it back
        channel.writeInbound(encodedMessage);
        ChatMessage decodedMessage = channel.readInbound();

        // Then - Message should be successfully decoded
        assertThat(decodedMessage).isNotNull();
        assertThat(decodedMessage.getPayload()).hasSize(payloadBytes.length);
    }

    @Test
    @DisplayName("Should reject messages with negative payload length")
    void testRejectNegativePayloadLength() {
        // Given - Create a message with negative payload length
        int negativePayloadLength = -1;
        
        // Create a valid header but with negative payload length
        PacketHeader header = new PacketHeader(
            PacketHeader.MAGIC,
            ProtocolMessageType.MESSAGE_SEND_REQ.getCode(),
            PacketHeader.VERSION,
            (byte) 0,
            PacketHeader.toCompactUuid(UUID.randomUUID()),
            negativePayloadLength,
            System.currentTimeMillis()
        );
        
        // Create buffer with header + dummy checksum
        ByteBuffer headerBuffer = ByteBuffer.allocate(PacketHeader.SIZE);
        header.write(headerBuffer);
        
        ByteBuf buffer = Unpooled.buffer(PacketHeader.SIZE + 4);
        buffer.writeBytes(headerBuffer.array());
        
        // Write dummy checksum
        buffer.writeInt(0);
        
        // When - Try to decode the message
        // Expect an exception to be thrown
        DecoderException exception = assertThrows(DecoderException.class, () -> {
            channel.writeInbound(buffer);
        });
        
        // Then - Exception should have the correct message
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Negative payload length");
        
        // Handler should still be in pipeline
        assertThat(channel.pipeline().context(BinaryMessageCodec.class)).isNotNull();
    }

    @Test
    @DisplayName("Should reject encoding of messages with payload size exceeding MAX_PAYLOAD_SIZE")
    void testRejectEncodingOversizedPayload() {
        // Given - Create a message with payload size exceeding MAX_PAYLOAD_SIZE
        byte[] oversizedPayload = new byte[ProtocolConstants.MAX_PAYLOAD_SIZE + 1];
        
        ChatMessage oversizedMessage = new ChatMessage(
            ProtocolMessageType.MESSAGE_SEND_REQ,
            (byte) 0,
            UUID.randomUUID(),
            System.currentTimeMillis(),
            oversizedPayload
        );
        
        // When - Try to encode the message
        // Expect an EncoderException wrapping IllegalArgumentException to be thrown
        EncoderException exception = assertThrows(EncoderException.class, () -> {
            channel.writeOutbound(oversizedMessage);
        });
        
        // Then - Exception should wrap an IllegalArgumentException with the correct message
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Payload size exceeds maximum allowed");
    }
}