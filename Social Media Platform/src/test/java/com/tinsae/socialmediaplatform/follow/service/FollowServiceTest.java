package com.tinsae.socialmediaplatform.follow.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.follow.entity.Follow;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private FollowService followService;

    @Test
    void followUserCreatesAcceptedFollow() {
        User follower = TestDataFactory.user("follower");
        User followed = TestDataFactory.user("followed");
        var jwt = TestDataFactory.jwt(follower);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(follower);
        when(userService.findActiveUserById(followed.getId())).thenReturn(followed);
        when(followRepository.findByFollowerIdAndFollowedId(follower.getId(), followed.getId()))
                .thenReturn(Optional.empty());
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> {
            Follow follow = invocation.getArgument(0);
            follow.setId(java.util.UUID.randomUUID());
            return follow;
        });

        var response = followService.followUser(followed.getId(), jwt);

        assertThat(response.followerId()).isEqualTo(follower.getId());
        assertThat(response.followedId()).isEqualTo(followed.getId());
        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void followUserRejectsSelfFollow() {
        User user = TestDataFactory.user("same-user");
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(userService.findActiveUserById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> followService.followUser(user.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("You cannot follow yourself.");
    }

    @Test
    void followUserRejectsDuplicateAcceptedFollow() {
        User follower = TestDataFactory.user("follower");
        User followed = TestDataFactory.user("followed");
        var jwt = TestDataFactory.jwt(follower);
        Follow existing = TestDataFactory.follow(follower, followed);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(follower);
        when(userService.findActiveUserById(followed.getId())).thenReturn(followed);
        when(followRepository.findByFollowerIdAndFollowedId(follower.getId(), followed.getId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> followService.followUser(followed.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("You are already following this user.");
    }

    @Test
    void unfollowUserDeletesRelationship() {
        User follower = TestDataFactory.user("follower");
        User followed = TestDataFactory.user("followed");
        var jwt = TestDataFactory.jwt(follower);
        Follow existing = TestDataFactory.follow(follower, followed);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(follower);
        when(userService.findActiveUserById(followed.getId())).thenReturn(followed);
        when(followRepository.findByFollowerIdAndFollowedId(follower.getId(), followed.getId()))
                .thenReturn(Optional.of(existing));

        followService.unfollowUser(followed.getId(), jwt);

        assertThat(existing.getFollowed().getId()).isEqualTo(followed.getId());
    }

    @Test
    void unfollowUserRejectsMissingRelationship() {
        User follower = TestDataFactory.user("follower");
        User followed = TestDataFactory.user("followed");
        var jwt = TestDataFactory.jwt(follower);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(follower);
        when(userService.findActiveUserById(followed.getId())).thenReturn(followed);
        when(followRepository.findByFollowerIdAndFollowedId(follower.getId(), followed.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollowUser(followed.getId(), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Follow relationship not found.");
    }

    @Test
    void getFollowStatusReturnsTrueForAcceptedFollow() {
        User follower = TestDataFactory.user("follower");
        User followed = TestDataFactory.user("followed");
        var jwt = TestDataFactory.jwt(follower);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(follower);
        when(userService.findActiveUserById(followed.getId())).thenReturn(followed);
        when(followRepository.existsByFollowerIdAndFollowedIdAndStatus(
                follower.getId(),
                followed.getId(),
                com.tinsae.socialmediaplatform.common.enums.FollowStatus.ACCEPTED
        )).thenReturn(true);

        var response = followService.getFollowStatus(followed.getId(), jwt);

        assertThat(response.following()).isTrue();
    }
}
