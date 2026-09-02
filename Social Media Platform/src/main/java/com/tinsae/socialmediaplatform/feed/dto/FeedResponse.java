package com.tinsae.socialmediaplatform.feed.dto;

import com.tinsae.socialmediaplatform.post.dto.PostResponse;

import java.util.List;

public record FeedResponse(
        List<PostResponse> items,
        String nextCursor
) {
}
