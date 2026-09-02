package com.tinsae.socialmediaplatform.follow.repository;

import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);

    List<Follow> findByFollowerIdAndStatus(UUID followerId, FollowStatus status);

    List<Follow> findByFollowedIdAndStatus(UUID followedId, FollowStatus status);

    boolean existsByFollowerIdAndFollowedIdAndStatus(UUID followerId, UUID followedId, FollowStatus status);
}
