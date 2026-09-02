package com.tinsae.socialmediaplatform.post.repository;

import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(UUID authorId, PostStatus status, Pageable pageable);

    @Query("""
            select p from Post p
            join fetch p.author author
            left join fetch author.profile
            where p.status = :status
              and (
                    p.visibility = :publicVisibility
                    or p.author.id = :viewerId
                    or (
                        p.visibility = :followersOnlyVisibility
                        and p.author.id in :followedAuthorIds
                    )
              )
              and (:cursor is null or p.createdAt < :cursor)
            order by p.createdAt desc
            """)
    List<Post> findFeedPosts(
            @Param("viewerId") UUID viewerId,
            @Param("followedAuthorIds") Collection<UUID> followedAuthorIds,
            @Param("status") PostStatus status,
            @Param("publicVisibility") PostVisibility publicVisibility,
            @Param("followersOnlyVisibility") PostVisibility followersOnlyVisibility,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );
}
