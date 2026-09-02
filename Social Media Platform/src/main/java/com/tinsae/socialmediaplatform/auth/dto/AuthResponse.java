package com.tinsae.socialmediaplatform.auth.dto;

import com.tinsae.socialmediaplatform.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserResponse user
) {
}
