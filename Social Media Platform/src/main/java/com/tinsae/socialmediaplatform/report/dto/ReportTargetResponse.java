package com.tinsae.socialmediaplatform.report.dto;

import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportTargetResponse(
        UUID reportId,
        ReportTargetType targetType,
        UUID targetId,
        UserSummaryResponse user,
        String email,
        UserStatus userStatus,
        String bio,
        UserSummaryResponse author,
        UserSummaryResponse sender,
        UserSummaryResponse recipient,
        UserSummaryResponse uploader,
        UUID postId,
        String content,
        String status,
        String visibility,
        Long commentCount,
        Long reactionCount,
        String url,
        MediaType mediaType,
        String mimeType,
        Long sizeBytes,
        String altText,
        Instant createdAt,
        List<MediaPreviewResponse> media
) {
    public record MediaPreviewResponse(
            UUID id,
            String url,
            MediaType mediaType,
            String mimeType,
            Long sizeBytes,
            String altText
    ) {
    }
}
