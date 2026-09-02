package com.tinsae.socialmediaplatform.media.dto;

import com.tinsae.socialmediaplatform.common.enums.MediaType;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        String url,
        MediaType mediaType,
        String mimeType,
        Long sizeBytes,
        String altText,
        Instant createdAt
) {
}
