package com.tinsae.socialmediaplatform.profile.dto;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String displayName,
        String bio,
        String avatarUrl,
        String coverImageUrl,
        String street,
        String city,
        String state,
        String country,
        String websiteUrl
) {
}
