package com.chatv2.common.dto;

import com.chatv2.common.model.UserProfile;

/**
 * Register request DTO.
 * Contains user profile information for registration.
 */
public record RegisterRequest(
    String username,
    String password,
    String fullName,
    String bio
) {
}
