package com.tinsae.socialmediaplatform.follow.service;

import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.mapper.FollowMapper;
import com.tinsae.socialmediaplatform.follow.dto.FollowResponse;
import com.tinsae.socialmediaplatform.follow.dto.FollowStatusResponse;
import com.tinsae.socialmediaplatform.follow.entity.Follow;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    public FollowService(
            FollowRepository followRepository,
            NotificationService notificationService,
            UserService userService
    ) {
        this.followRepository = followRepository;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Transactional
    public FollowResponse followUser(UUID targetUserId, Jwt jwt) {
        User follower = userService.getAuthenticatedUser(jwt);
        User followed = userService.findActiveUserById(targetUserId);

        if (follower.getId().equals(followed.getId())) {
            throw new BusinessRuleException("You cannot follow yourself.");
        }

        Follow follow = followRepository.findByFollowerIdAndFollowedId(follower.getId(), followed.getId())
                .map(existingFollow -> {
                    if (existingFollow.getStatus() == FollowStatus.ACCEPTED) {
                        throw new BusinessRuleException("You are already following this user.");
                    }
                    return existingFollow;
                })
                .orElseGet(() -> {
                    Follow newFollow = new Follow();
                    newFollow.setFollower(follower);
                    newFollow.setFollowed(followed);
                    return newFollow;
                });

        follow.setStatus(FollowStatus.ACCEPTED);
        Follow savedFollow = followRepository.save(follow);
        notificationService.createNotification(
                followed,
                follower,
                NotificationType.FOLLOW,
                "New follower",
                follower.getUsername() + " started following you.",
                "USER",
                follower.getId()
        );
        return FollowMapper.toResponse(savedFollow);
    }

    @Transactional
    public void unfollowUser(UUID targetUserId, Jwt jwt) {
        User follower = userService.getAuthenticatedUser(jwt);
        User followed = userService.findActiveUserById(targetUserId);

        Follow follow = followRepository.findByFollowerIdAndFollowedId(follower.getId(), followed.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found."));

        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(UUID targetUserId, Jwt jwt) {
        User follower = userService.getAuthenticatedUser(jwt);
        User followed = userService.findActiveUserById(targetUserId);

        boolean following = followRepository.existsByFollowerIdAndFollowedIdAndStatus(
                follower.getId(),
                followed.getId(),
                FollowStatus.ACCEPTED
        );

        return new FollowStatusResponse(following);
    }
}
