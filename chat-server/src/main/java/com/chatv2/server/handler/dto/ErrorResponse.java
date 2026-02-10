package com.chatv2.server.handler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for error response payload.
 * JSON format: {"error": "error message", "code": "error_code"}
 */
public record ErrorResponse(
    @JsonProperty("error") String error,
    @JsonProperty("code") String code
) {
    /**
     * Creates an error response with default code.
     *
     * @param error error message
     * @return ErrorResponse instance
     */
    public static ErrorResponse create(String error) {
        return new ErrorResponse(error, "AUTH_ERROR");
    }

    /**
     * Creates an error response with specific code.
     *
     * @param error error message
     * @param code error code
     * @return ErrorResponse instance
     */
    public static ErrorResponse create(String error, String code) {
        return new ErrorResponse(error, code);
    }
}
