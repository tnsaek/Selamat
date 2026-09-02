package com.tinsae.socialmediaplatform.user.dto;

import com.tinsae.socialmediaplatform.common.enums.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UserStatus status,
        Set<String> roles,
        Instant createdAt
) {
}
