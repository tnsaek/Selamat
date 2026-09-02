package com.tinsae.socialmediaplatform.post.dto;

import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @Size(max = 5000, message = "Post content must not exceed 5000 characters.")
        String content,

        PostVisibility visibility
) {
}
