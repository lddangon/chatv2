package com.chatv2.common.dto;

/**
 * Logout request DTO.
 * Contains the session token for logout.
 */
public record LogoutRequest(
    String token
) {
}
