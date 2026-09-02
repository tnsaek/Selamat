package com.tinsae.socialmediaplatform.reaction.repository;

import com.tinsae.socialmediaplatform.reaction.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    Optional<Reaction> findByUserIdAndPostId(UUID userId, UUID postId);

    Optional<Reaction> findByUserIdAndCommentId(UUID userId, UUID commentId);

    long countByPostId(UUID postId);

    long countByCommentId(UUID commentId);
}
