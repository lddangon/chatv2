package com.chatv2.common.protocol;

import com.chatv2.common.protocol.MessageCodec;
import com.chatv2.common.protocol.ProtocolMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EmptySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageCodecTest {

    @Test
    @DisplayName("Should encode object to JSON string")
    void testEncode() throws IOException {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "value1");
        payload.put("key2", 123);
        payload.put("key3", true);

        // When
        String json = MessageCodec.encode(payload);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).isNotBlank();
        assertThat(json).contains("\"key1\":\"value1\"");
        assertThat(json).contains("\"key2\":123");
        assertThat(json).contains("\"key3\":true");
    }

    @Test
    @DisplayName("Should return string as is when encoding string")
    void testEncodeString() throws IOException {
        // Given
        String payload = "{\"key\":\"value\"}";

        // When
        String result = MessageCodec.encode(payload);

        // Then
        assertThat(result).isEqualTo(payload);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Should throw IllegalArgumentException when encoding null payload")
    void testEncodeNull(Object payload) {
        // When/Then
        assertThatThrownBy(() -> MessageCodec.encode(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload cannot be null");
    }

    @Test
    @DisplayName("Should decode JSON string to object")
    void testDecode() throws IOException {
        // Given
        String json = "{\"key1\":\"value1\",\"key2\":123,\"key3\":true}";
        Class<Map> targetType = Map.class;

        // When
        Map<String, Object> result = MessageCodec.decode(json, targetType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(123);
        assertThat(result.get("key3")).isEqualTo(true);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when decoding null or blank JSON")
    void testDecodeInvalidJson(String json) {
        // Given
        Class<Map> targetType = Map.class;

        // When/Then
        assertThatThrownBy(() -> MessageCodec.decode(json, targetType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JSON string cannot be null or blank");
    }

    @Test
    @DisplayName("Should decode JSON string to JsonNode")
    void testDecodeToJson() throws IOException {
        // Given
        String json = "{\"key1\":\"value1\",\"key2\":123,\"key3\":true}";

        // When
        JsonNode jsonNode = MessageCodec.decodeToJson(json);

        // Then
        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.get("key1").asText()).isEqualTo("value1");
        assertThat(jsonNode.get("key2").asInt()).isEqualTo(123);
        assertThat(jsonNode.get("key3").asBoolean()).isEqualTo(true);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @DisplayName("Should throw IllegalArgumentException when decoding null or blank JSON to JsonNode")
    void testDecodeToJsonInvalidJson(String json) {
        // When/Then
        assertThatThrownBy(() -> MessageCodec.decodeToJson(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JSON string cannot be null or blank");
    }

    @Test
    @DisplayName("Should encode object to bytes")
    void testEncodeToBytes() throws IOException {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "value1");
        payload.put("key2", 123);

        // When
        byte[] bytes = MessageCodec.encodeToBytes(payload);

        // Then
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
        
        String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json).contains("\"key1\":\"value1\"");
        assertThat(json).contains("\"key2\":123");
    }

    @Test
    @DisplayName("Should decode bytes to object")
    void testDecodeFromBytes() throws IOException {
        // Given
        String json = "{\"key1\":\"value1\",\"key2\":123}";
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Class<Map> targetType = Map.class;

        // When
        Map<String, Object> result = MessageCodec.decodeFromBytes(bytes, targetType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(123);
    }

    @Test
    @DisplayName("Should encode and decode bytes consistently")
    void testEncodeDecodeBytesConsistency() throws IOException {
        // Given
        Map<String, Object> originalPayload = new HashMap<>();
        originalPayload.put("key1", "value1");
        originalPayload.put("key2", 123);
        originalPayload.put("key3", true);
        Class<Map> targetType = Map.class;

        // When
        byte[] encodedBytes = MessageCodec.encodeToBytes(originalPayload);
        Map<String, Object> decodedPayload = MessageCodec.decodeFromBytes(encodedBytes, targetType);

        // Then
        assertThat(decodedPayload).isEqualTo(originalPayload);
    }

    @Test
    @DisplayName("Should encode and decode complex objects consistently")
    void testEncodeDecodeComplexObjectConsistency() throws IOException {
        // Given
        Map<String, Object> originalPayload = new HashMap<>();
        originalPayload.put("senderId", UUID.randomUUID().toString());
        originalPayload.put("receiverId", UUID.randomUUID().toString());
        originalPayload.put("content", "Hello, world!");
        originalPayload.put("messageType", ProtocolMessageType.MESSAGE_SEND_REQ.name());
        Class<Map> targetType = Map.class;

        // When
        String json = MessageCodec.encode(originalPayload);
        Map<String, Object> decodedPayload = MessageCodec.decode(json, targetType);

        // Then
        assertThat(decodedPayload).isEqualTo(originalPayload);
    }

    @Test
    @DisplayName("Should return ObjectMapper instance")
    void testGetObjectMapper() {
        // When
        var objectMapper = MessageCodec.getObjectMapper();

        // Then
        assertThat(objectMapper).isNotNull();
    }

    @Test
    @DisplayName("Should generate random UUID")
    void testGenerateRandomUUID() {
        // Given
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        // When
        String uuid1Str = uuid1.toString();
        String uuid2Str = uuid2.toString();

        // Then
        assertThat(uuid1Str).isNotNull();
        assertThat(uuid2Str).isNotNull();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    @DisplayName("Should throw exception when creating invalid UUID")
    void testInvalidUUID() {
        // Given
        String invalidUuid = "not-a-uuid";

        // When/Then
        assertThatThrownBy(() -> UUID.fromString(invalidUuid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reuse ObjectMapper instance across calls")
    void testGetObjectMapperReturnsSameInstance() {
        // When
        var objectMapper1 = MessageCodec.getObjectMapper();
        var objectMapper2 = MessageCodec.getObjectMapper();

        // Then
        assertThat(objectMapper1).isSameAs(objectMapper2);
    }

    @Test
    @DisplayName("Should handle nested objects")
    void testEncodeDecodeNestedObjects() throws IOException {
        // Given
        Map<String, Object> innerObject = new HashMap<>();
        innerObject.put("innerKey1", "innerValue1");
        innerObject.put("innerKey2", 456);

        Map<String, Object> outerObject = new HashMap<>();
        outerObject.put("outerKey1", "outerValue1");
        outerObject.put("outerKey2", 789);
        outerObject.put("nestedObject", innerObject);

        // When
        String json = MessageCodec.encode(outerObject);
        Map<String, Object> decodedOuter = MessageCodec.decode(json, Map.class);

        // Then
        assertThat(decodedOuter).isNotNull();
        assertThat(decodedOuter.get("outerKey1")).isEqualTo("outerValue1");
        assertThat(decodedOuter.get("outerKey2")).isEqualTo(789);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> decodedInner = (Map<String, Object>) decodedOuter.get("nestedObject");
        assertThat(decodedInner.get("innerKey1")).isEqualTo("innerValue1");
        assertThat(decodedInner.get("innerKey2")).isEqualTo(456);
    }

    @Test
    @DisplayName("Should handle arrays in JSON")
    void testEncodeDecodeArrays() throws IOException {
        // Given
        String[] stringArray = {"item1", "item2", "item3"};
        Map<String, Object> payload = new HashMap<>();
        payload.put("arrayField", stringArray);

        // When
        String json = MessageCodec.encode(payload);
        Map<String, Object> decoded = MessageCodec.decode(json, Map.class);

        // Then
        assertThat(decoded).isNotNull();
        
        @SuppressWarnings("unchecked")
        java.util.List<String> decodedArray = (java.util.List<String>) decoded.get("arrayField");
        assertThat(decodedArray).containsExactly("item1", "item2", "item3");
    }
}