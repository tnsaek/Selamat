package com.tinsae.socialmediaplatform.reaction.dto;

import com.tinsae.socialmediaplatform.common.enums.ReactionType;

import java.time.Instant;
import java.util.UUID;

public record ReactionResponse(
        UUID id,
        UUID userId,
        UUID postId,
        UUID commentId,
        ReactionType reactionType,
        Instant createdAt
) {
}
