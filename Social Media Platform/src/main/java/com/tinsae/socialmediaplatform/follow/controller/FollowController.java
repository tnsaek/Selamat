package com.tinsae.socialmediaplatform.follow.controller;

import com.tinsae.socialmediaplatform.follow.dto.FollowResponse;
import com.tinsae.socialmediaplatform.follow.dto.FollowStatusResponse;
import com.tinsae.socialmediaplatform.follow.service.FollowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @GetMapping
    public FollowStatusResponse getFollowStatus(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return followService.getFollowStatus(userId, jwt);
    }

    @PostMapping
    public ResponseEntity<FollowResponse> followUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followService.followUser(userId, jwt));
    }

    @DeleteMapping
    public ResponseEntity<Void> unfollowUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        followService.unfollowUser(userId, jwt);
        return ResponseEntity.noContent().build();
    }
}
