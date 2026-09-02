package com.tinsae.socialmediaplatform.comment.dto;

import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID parentCommentId,
        UserSummaryResponse author,
        String content,
        CommentStatus status,
        Long reactionCount,
        Instant createdAt
) {
}
