package com.chatv2.common.dto;

/**
 * Login request DTO.
 * Contains username and password for authentication.
 */
public record LoginRequest(
    String username,
    String password
) {
}
