package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.post.dto.PostResponse;
import com.tinsae.socialmediaplatform.post.entity.Post;

import java.util.List;

public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toResponse(Post post, List<MediaResponse> media, ReactionType viewerReaction) {
        return new PostResponse(
                post.getId(),
                UserMapper.toSummary(post.getAuthor()),
                post.getContent(),
                post.getVisibility(),
                post.getStatus(),
                media,
                post.getCommentCount(),
                post.getReactionCount(),
                viewerReaction,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
