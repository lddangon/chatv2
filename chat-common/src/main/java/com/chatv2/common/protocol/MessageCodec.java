package com.chatv2.common.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JSON codec for encoding and decoding protocol messages.
 * Provides thread-safe JSON serialization and deserialization.
 */
public class MessageCodec {
    private static final Logger log = LoggerFactory.getLogger(MessageCodec.class);
    
    private static volatile ObjectMapper objectMapper;
    
    /**
     * Gets the shared ObjectMapper instance.
     * Thread-safe lazy initialization.
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            synchronized (MessageCodec.class) {
                if (objectMapper == null) {
                    objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    log.debug("ObjectMapper initialized");
                }
            }
        }
        return objectMapper;
    }
    
    /**
     * Encodes an object to JSON string.
     *
     * @param payload object to encode
     * @return JSON string representation
     * @throws IllegalArgumentException if payload is null
     * @throws IOException if encoding fails
     */
    public static String encode(Object payload) throws IOException {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
        
        // If payload is already a string, return as is
        if (payload instanceof String) {
            return (String) payload;
        }
        
        return getObjectMapper().writeValueAsString(payload);
    }
    
    /**
     * Decodes a JSON string to an object of the specified type.
     *
     * @param json JSON string to decode
     * @param targetType target class type
     * @param <T> type parameter
     * @return decoded object
     * @throws IllegalArgumentException if JSON is null or blank
     * @throws IOException if decoding fails
     */
    public static <T> T decode(String json, Class<T> targetType) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or blank");
        }
        
        return getObjectMapper().readValue(json, targetType);
    }
    
    /**
     * Decodes a JSON string to JsonNode.
     *
     * @param json JSON string to decode
     * @return JsonNode representation
     * @throws IllegalArgumentException if JSON is null or blank
     * @throws IOException if decoding fails
     */
    public static JsonNode decodeToJson(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or blank");
        }
        
        return getObjectMapper().readTree(json);
    }
    
    /**
     * Encodes an object to byte array using UTF-8 encoding.
     *
     * @param payload object to encode
     * @return byte array containing JSON
     * @throws IllegalArgumentException if payload is null
     * @throws IOException if encoding fails
     */
    public static byte[] encodeToBytes(Object payload) throws IOException {
        String json = encode(payload);
        return json.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Decodes a byte array to an object of the specified type.
     *
     * @param bytes byte array containing JSON
     * @param targetType target class type
     * @param <T> type parameter
     * @return decoded object
     * @throws IllegalArgumentException if bytes is null
     * @throws IOException if decoding fails
     */
    public static <T> T decodeFromBytes(byte[] bytes, Class<T> targetType) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes cannot be null");
        }
        
        String json = new String(bytes, StandardCharsets.UTF_8);
        return decode(json, targetType);
    }
}
