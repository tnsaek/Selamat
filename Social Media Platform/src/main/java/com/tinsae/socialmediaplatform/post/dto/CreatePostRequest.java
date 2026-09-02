package com.tinsae.socialmediaplatform.post.dto;

import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePostRequest(
        @Size(max = 5000, message = "Post content must not exceed 5000 characters.")
        String content,

        @NotNull(message = "Post visibility is required.")
        PostVisibility visibility,

        List<UUID> mediaIds
) {
}
