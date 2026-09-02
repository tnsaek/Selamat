package com.tinsae.socialmediaplatform.post.dto;

import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UserSummaryResponse author,
        String content,
        PostVisibility visibility,
        PostStatus status,
        List<MediaResponse> media,
        Long commentCount,
        Long reactionCount,
        ReactionType viewerReaction,
        Instant createdAt,
        Instant updatedAt
) {
}
