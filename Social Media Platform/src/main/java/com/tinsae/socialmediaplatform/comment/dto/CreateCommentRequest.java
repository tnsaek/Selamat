package com.tinsae.socialmediaplatform.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommentRequest(
        @NotBlank(message = "Comment content is required.")
        @Size(max = 2000, message = "Comment content must not exceed 2000 characters.")
        String content,

        UUID parentCommentId
) {
}
