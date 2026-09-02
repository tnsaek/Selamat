package com.tinsae.socialmediaplatform.follow.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.follow.dto.FollowResponse;
import com.tinsae.socialmediaplatform.follow.dto.FollowStatusResponse;
import com.tinsae.socialmediaplatform.follow.service.FollowService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowControllerTest {

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowController followController;

    @Test
    void followUserReturnsCreatedResponse() {
        User follower = TestDataFactory.user("follower");
        var jwt = TestDataFactory.jwt(follower);
        UUID followedId = UUID.randomUUID();
        FollowResponse follow = new FollowResponse(
                UUID.randomUUID(),
                follower.getId(),
                followedId,
                FollowStatus.ACCEPTED,
                Instant.now()
        );
        when(followService.followUser(followedId, jwt)).thenReturn(follow);

        var response = followController.followUser(followedId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(follow);
        verify(followService).followUser(followedId, jwt);
    }

    @Test
    void getFollowStatusDelegatesToFollowService() {
        User follower = TestDataFactory.user("follower");
        var jwt = TestDataFactory.jwt(follower);
        UUID followedId = UUID.randomUUID();
        FollowStatusResponse status = new FollowStatusResponse(true);
        when(followService.getFollowStatus(followedId, jwt)).thenReturn(status);

        FollowStatusResponse response = followController.getFollowStatus(followedId, jwt);

        assertThat(response).isSameAs(status);
        verify(followService).getFollowStatus(followedId, jwt);
    }

    @Test
    void unfollowUserReturnsNoContent() {
        User follower = TestDataFactory.user("follower");
        var jwt = TestDataFactory.jwt(follower);
        UUID followedId = UUID.randomUUID();

        var response = followController.unfollowUser(followedId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(followService).unfollowUser(followedId, jwt);
    }
}
