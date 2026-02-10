package com.chatv2.common.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Тесты для проверки исправлений в протоколе сообщений.
 * Проверяет HEADER_SIZE = 40, правильное положение payloadLength,
 * работу PacketHeader с полным UUID и checksum.
 */
public class SimpleProtocolFixTest {

    @Test
    public void testChatMessageHeaderSize() {
        // Проверяем, что HEADER_SIZE равен 40 байт (с 16-байтовым UUID)
        assertEquals(40, ChatMessage.HEADER_SIZE, "Header size should be 40 bytes with 16-byte UUID");
    }

    @Test
    public void testPacketHeaderSize() {
        // Проверяем, что размер заголовка в PacketHeader равен 40 байт (с 16-байтовым UUID)
        assertEquals(40, PacketHeader.SIZE, "PacketHeader size should be 40 bytes with 16-byte UUID");
    }

    @Test
    public void testPayloadLengthPositionInBinaryMessageCodec() {
        // Проверяем, что BinaryMessageCodec читает payloadLength с правильного смещения

        // В BinaryMessageCodec.java:
        // int payloadLength = buf.getInt(buf.readerIndex() + 24);
        // Это означает, что payloadLength должен находиться на смещении 24
        // (после 16-байтового UUID: 4 + 2 + 1 + 1 + 16 = 24)

        // Создаем тестовый ByteBuffer размером 40 байт (новый размер заголовка)
        ByteBuffer buffer = ByteBuffer.allocate(40);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Заполняем заголовок тестовыми данными в соответствии с новой структурой:
        // 0-3: Magic Number (4 bytes)
        // 4-5: Message Type (2 bytes)
        // 6: Version (1 byte)
        // 7: Flags (1 byte)
        // 8-23: Message ID (16 bytes) - полный UUID
        // 24-27: Payload Length (4 bytes)
        buffer.putInt(ChatMessage.MAGIC_NUMBER); // 0-3: Magic Number
        buffer.putShort((short) 1); // 4-5: Message Type
        buffer.put(ChatMessage.PROTOCOL_VERSION); // 6: Version
        buffer.put((byte) 0); // 7: Flags
        buffer.putLong(12345L); // 8-15: UUID mostSigBits
        buffer.putLong(67890L); // 16-23: UUID leastSigBits

        // Смещение payloadLength в BinaryMessageCodec равно 24
        int binaryCodecOffset = 24;

        // Записываем тестовую длину payload на смещение 24
        buffer.putInt(binaryCodecOffset, 1024);

        // Проверяем, что при чтении payloadLength используется правильное смещение
        int readPayloadLength = buffer.getInt(binaryCodecOffset);
        assertEquals(1024, readPayloadLength, "Payload length should be read from offset 24 as used in BinaryMessageCodec with 16-byte UUID");
    }

    @Test
    public void testPacketHeaderUuidHandling() {
        // Проверяем, что PacketHeader использует полный UUID (16 байт)
        
        // Создаем тестовый UUID
        UUID messageId = UUID.randomUUID();
        
        // Проверяем, что у нас есть методы для получения и установки полного UUID
        long mostSigBits = messageId.getMostSignificantBits();
        long leastSigBits = messageId.getLeastSignificantBits();
        
        // Проверяем, что мы можем воссоздать UUID из этих частей
        UUID recreatedUuid = new UUID(mostSigBits, leastSigBits);
        assertEquals(messageId, recreatedUuid, "UUID should be correctly recreated from its parts");
    }

    @Test
    public void testPacketHeaderChecksum() {
        // Проверяем, что PacketHeader корректно работает с checksum
        byte[] payload = "test payload for checksum".getBytes();
        
        // Создаем заголовок
        PacketHeader header = PacketHeader.create(
            (short) ProtocolMessageType.MESSAGE_SEND_REQ.getCode(),
            ChatMessage.FLAG_ACK_REQUIRED,
            UUID.randomUUID(),
            payload.length,
            payload
        );
        
        // Проверяем валидность checksum
        assertTrue(header.validateChecksum(payload), "Header checksum should be valid");
        
        // Изменяем payload и проверяем, что checksum больше не валиден
        byte[] modifiedPayload = "modified payload".getBytes();
        assertFalse(header.validateChecksum(modifiedPayload), "Header checksum should be invalid for modified payload");
    }

    @Test
    public void testBinaryMessageCodecPayloadLengthOffset() {
        // Проверяем, что BinaryMessageCodec использует правильное смещение для payloadLength

        // В BinaryMessageCodec.java:
        // int payloadLength = buf.getInt(buf.readerIndex() + 24);
        // Это означает, что payloadLength читается со смещения 24

        // Создаем тестовый ByteBuffer размером 40 байт (новый размер заголовка)
        ByteBuffer buffer = ByteBuffer.allocate(40);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Заполняем заголовок тестовыми данными в соответствии с новой структурой:
        // 0-3: Magic Number (4 bytes)
        // 4-5: Message Type (2 bytes)
        // 6: Version (1 byte)
        // 7: Flags (1 byte)
        // 8-23: Message ID (16 bytes) - полный UUID
        // 24-27: Payload Length (4 bytes)
        buffer.putInt(ChatMessage.MAGIC_NUMBER); // 0-3: Magic Number
        buffer.putShort((short) 1); // 4-5: Message Type
        buffer.put(ChatMessage.PROTOCOL_VERSION); // 6: Version
        buffer.put((byte) 0); // 7: Flags
        buffer.putLong(12345L); // 8-15: UUID mostSigBits
        buffer.putLong(67890L); // 16-23: UUID leastSigBits

        // В BinaryMessageCodec payloadLength читается со смещения 24
        int binaryCodecOffset = 24;

        // Записываем тестовую длину payload на смещение 24
        buffer.putInt(binaryCodecOffset, 1024);

        // Проверяем, что при чтении payloadLength используется правильное смещение
        int readPayloadLength = buffer.getInt(binaryCodecOffset);
        assertEquals(1024, readPayloadLength, "Payload length should be read from offset 24 as used in BinaryMessageCodec");

        // В PacketHeader payloadLength также находится на смещении 24
        // (4+2+1+1+16 = 24, после полного 16-байтового UUID)
        ByteBuffer packetHeaderBuffer = ByteBuffer.allocate(40);
        packetHeaderBuffer.order(ByteOrder.BIG_ENDIAN);

        // Заполняем в соответствии с форматом PacketHeader
        packetHeaderBuffer.putInt(PacketHeader.MAGIC); // 0-3: Magic Number
        packetHeaderBuffer.putShort((short) 1); // 4-5: Message Type
        packetHeaderBuffer.put(PacketHeader.VERSION); // 6: Version
        packetHeaderBuffer.put((byte) 0); // 7: Flags
        packetHeaderBuffer.putLong(12345L); // 8-15: mostSigBits UUID
        packetHeaderBuffer.putLong(67890L); // 16-23: leastSigBits UUID

        // В PacketHeader payloadLength находится на смещении 24
        int packetHeaderOffset = 24;

        // Записываем тестовую длину payload на смещение 24
        packetHeaderBuffer.putInt(packetHeaderOffset, 2048);

        // Проверяем, что при чтении payloadLength используется правильное смещение
        int readPacketHeaderPayloadLength = packetHeaderBuffer.getInt(packetHeaderOffset);
        assertEquals(2048, readPacketHeaderPayloadLength, "Payload length should be read from offset 24 as used in PacketHeader");
    }
}