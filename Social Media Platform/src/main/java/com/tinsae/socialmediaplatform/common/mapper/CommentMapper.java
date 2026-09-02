package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.comment.dto.CommentResponse;
import com.tinsae.socialmediaplatform.comment.entity.Comment;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                UserMapper.toSummary(comment.getAuthor()),
                comment.getContent(),
                comment.getStatus(),
                comment.getReactionCount(),
                comment.getCreatedAt()
        );
    }
}
