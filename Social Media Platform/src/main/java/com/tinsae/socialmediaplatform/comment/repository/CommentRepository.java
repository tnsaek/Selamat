package com.tinsae.socialmediaplatform.comment.repository;

import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @Query("""
            select c from Comment c
            where c.post.id = :postId
              and c.status = :status
              and (:cursor is null or c.createdAt > :cursor)
            order by c.createdAt asc
            """)
    List<Comment> findVisiblePostComments(
            @Param("postId") UUID postId,
            @Param("status") CommentStatus status,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );
}
