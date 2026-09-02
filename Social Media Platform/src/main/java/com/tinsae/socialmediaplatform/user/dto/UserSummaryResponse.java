package com.tinsae.socialmediaplatform.user.dto;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {
}
