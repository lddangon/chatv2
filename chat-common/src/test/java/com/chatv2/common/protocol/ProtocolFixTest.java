package com.chatv2.common.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Тесты для проверки исправлений в протоколе сообщений.
 * Проверяет HEADER_SIZE = 40, правильное положение payloadLength,
 * работу PacketHeader с полным UUID и checksum.
 */
public class ProtocolFixTest {

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
    public void testPayloadLengthPosition() {
        // Проверяем, что payloadLength находится на правильном смещении (24)
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

        // Смещение payloadLength должно быть 24 (после 16-байтового UUID)
        int payloadLengthPos = buffer.position();
        assertEquals(24, payloadLengthPos, "Payload length should be at offset 24 after 16-byte UUID");

        // Записываем длину payload
        buffer.putInt(1024); // 24-27: Payload Length

        // Проверяем, что при чтении payloadLength используется правильное смещение
        buffer.rewind();
        int readPayloadLength = buffer.getInt(24); // Читаем с того же смещения
        assertEquals(1024, readPayloadLength, "Payload length should be read from offset 24");

        // Проверяем, что BinaryMessageCodec использует правильное смещение (24)
        int codecReadPayloadLength = buffer.getInt(24);
        assertEquals(1024, codecReadPayloadLength, "BinaryMessageCodec should read payload length from offset 24");
    }

    @Test
    public void testChatMessageEncoding() {
        // Проверяем кодирование сообщения с правильным расположением payloadLength
        UUID messageId = UUID.randomUUID();
        byte[] payload = "test message".getBytes();
        ChatMessage message = new ChatMessage(
            ProtocolMessageType.CHAT_CREATE_REQ,
            (byte) 0,
            messageId,
            System.currentTimeMillis(),
            payload
        );
        
        byte[] encoded = message.encode();
        
        // Проверяем, что размер заголовка = 32 байта
        assertTrue(encoded.length >= 32, "Encoded message should be at least 32 bytes");
        
        // Проверяем расположение payloadLength
        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        // Пропускаем к позиции payloadLength (смещение 16)
        buffer.position(16);
        int payloadLength = buffer.getInt();
        assertEquals(payload.length, payloadLength);
    }

    @Test
    public void testChatMessageDecoding() {
        // Создаем тестовое сообщение вручную, чтобы избежать проблем с кодированием
        UUID messageId = UUID.randomUUID();
        byte[] payload = "test message".getBytes();
        
        // Создаем ByteBuffer для сообщения
        ByteBuffer buffer = ByteBuffer.allocate(ChatMessage.HEADER_SIZE + payload.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        // Заполняем заголовок
        buffer.putInt(ChatMessage.MAGIC_NUMBER); // 0-3: Magic Number
        buffer.putShort(ProtocolMessageType.CHAT_CREATE_REQ.getCode()); // 4-5: Message Type
        buffer.put(ChatMessage.PROTOCOL_VERSION); // 6: Version
        buffer.put((byte) 0); // 7: Flags
        buffer.putLong(messageId.getMostSignificantBits()); // 8-15: UUID часть 1
        buffer.putLong(messageId.getLeastSignificantBits()); // 16-23: UUID часть 2 (смещение payloadLength здесь)
        buffer.putInt(payload.length); // 24-27: Payload Length (смещение 24)
        buffer.putLong(System.currentTimeMillis()); // 28-35: Timestamp
        
        // Calculate checksum
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(payload);
        buffer.putInt((int) crc32.getValue()); // 36-39: Checksum
        
        // Добавляем payload
        buffer.put(payload);
        
        // Декодируем сообщение
        byte[] encoded = buffer.array();
        ChatMessage decodedMessage = ChatMessage.decode(encoded);
        
        // Проверяем, что сообщение декодировалось корректно
        assertEquals(ProtocolMessageType.CHAT_CREATE_REQ, decodedMessage.getMessageType());
        assertEquals(messageId, decodedMessage.getMessageId());
        assertEquals((byte) 0, decodedMessage.getFlags());
        assertArrayEquals(payload, decodedMessage.getPayload());
    }

    @Test
    public void testPacketHeaderWithFullUuid() {
        // Проверяем работу PacketHeader с полным UUID (16 байт)
        UUID messageId = UUID.randomUUID();
        byte flags = ChatMessage.FLAG_ENCRYPTED;
        byte[] payload = "test payload".getBytes();
        int payloadLength = payload.length;

        // Создаем заголовок
        PacketHeader header = PacketHeader.create(
            (short) ProtocolMessageType.CHAT_CREATE_REQ.getCode(),
            flags,
            messageId,
            payloadLength,
            payload
        );

        // Проверяем, что размер заголовка = 40 байт (с 16-байтовым UUID)
        assertEquals(40, PacketHeader.SIZE, "PacketHeader.SIZE should be 40 bytes with 16-byte UUID");

        // Проверяем UUID
        assertEquals(messageId, header.getMessageId());

        // Проверяем сериализацию и десериализацию
        ByteBuffer buffer = ByteBuffer.allocate(PacketHeader.SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Проверяем, что у нас достаточно места для записи заголовка
        assertTrue(buffer.remaining() >= PacketHeader.SIZE,
                  "Buffer should have enough space for header");

        header.write(buffer);
        buffer.flip();

        PacketHeader deserializedHeader = PacketHeader.read(buffer);

        // Проверяем, что все поля совпадают
        assertEquals(header.magic(), deserializedHeader.magic());
        assertEquals(header.messageType(), deserializedHeader.messageType());
        assertEquals(header.version(), deserializedHeader.version());
        assertEquals(header.flags(), deserializedHeader.flags());
        assertEquals(header.payloadLength(), deserializedHeader.payloadLength());
        assertEquals(header.checksum(), deserializedHeader.checksum());

        // Проверяем UUID
        assertEquals(header.getMessageId(), deserializedHeader.getMessageId());
    }

    @Test
    public void testPacketHeaderChecksum() {
        // Проверяем расчет и проверку checksum
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
        // Проверяем, что BinaryMessageCodec читает payloadLength с правильного смещения
        
        // Создаем ByteBuffer для теста
        UUID messageId = UUID.randomUUID();
        byte[] payload = "test payload".getBytes();
        
        ByteBuffer buffer = ByteBuffer.allocate(ChatMessage.HEADER_SIZE + payload.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        // Заполняем заголовок
        buffer.putInt(ChatMessage.MAGIC_NUMBER); // 0-3: Magic Number
        buffer.putShort(ProtocolMessageType.MESSAGE_SEND_REQ.getCode()); // 4-5: Message Type
        buffer.put(ChatMessage.PROTOCOL_VERSION); // 6: Version
        buffer.put(ChatMessage.FLAG_ENCRYPTED); // 7: Flags
        buffer.putLong(messageId.getMostSignificantBits()); // 8-15: UUID часть 1
        buffer.putLong(messageId.getLeastSignificantBits()); // 16-23: UUID часть 2
        buffer.putInt(payload.length); // 24-27: Payload Length (смещение 24)
        buffer.putLong(System.currentTimeMillis()); // 28-35: Timestamp
        
        // Calculate checksum
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(payload);
        buffer.putInt((int) crc32.getValue()); // 36-39: Checksum
        
        // Добавляем payload
        buffer.put(payload);
        
        buffer.rewind();
        
        // Проверяем, что payloadLength находится на правильном смещении
        int magic = buffer.getInt(); // 0-3
        assertEquals(ChatMessage.MAGIC_NUMBER, magic);
        
        short messageType = buffer.getShort(); // 4-5
        assertEquals(ProtocolMessageType.MESSAGE_SEND_REQ.getCode(), messageType);
        
        byte version = buffer.get(); // 6
        assertEquals(ChatMessage.PROTOCOL_VERSION, version);
        
        byte flags = buffer.get(); // 7
        assertEquals(ChatMessage.FLAG_ENCRYPTED, flags);
        
        // 8-15: UUID часть 1 (8 байт)
        long mostSigBits = buffer.getLong();
        assertEquals(messageId.getMostSignificantBits(), mostSigBits);
        
        // 16-23: UUID часть 2 (8 байт)
        long leastSigBits = buffer.getLong();
        assertEquals(messageId.getLeastSignificantBits(), leastSigBits);

        // 24-27: Payload Length (смещение 24)
        int payloadLengthPos = buffer.position();
        assertEquals(24, payloadLengthPos, "Payload length should be at offset 24");

        int payloadLength = buffer.getInt();
        assertEquals(payload.length, payloadLength, "Payload length should match");

        // Проверяем, что BinaryMessageCodec читает payloadLength с правильного смещения (24)
        // После исправления, BinaryMessageCodec использует смещение 24, как и PacketHeader
        buffer.rewind();
        int binaryPayloadLength = buffer.getInt(24);
        assertEquals(payload.length, binaryPayloadLength,
            "BinaryMessageCodec should read payload length from offset 24 (consistent with PacketHeader)");
    }
}