package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.follow.dto.FollowResponse;
import com.tinsae.socialmediaplatform.follow.entity.Follow;

public final class FollowMapper {

    private FollowMapper() {
    }

    public static FollowResponse toResponse(Follow follow) {
        return new FollowResponse(
                follow.getId(),
                follow.getFollower().getId(),
                follow.getFollowed().getId(),
                follow.getStatus(),
                follow.getCreatedAt()
        );
    }
}
