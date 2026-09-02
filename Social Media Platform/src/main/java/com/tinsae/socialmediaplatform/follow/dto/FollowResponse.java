package com.tinsae.socialmediaplatform.follow.dto;

import com.tinsae.socialmediaplatform.common.enums.FollowStatus;

import java.time.Instant;
import java.util.UUID;

public record FollowResponse(
        UUID id,
        UUID followerId,
        UUID followedId,
        FollowStatus status,
        Instant createdAt
) {
}
