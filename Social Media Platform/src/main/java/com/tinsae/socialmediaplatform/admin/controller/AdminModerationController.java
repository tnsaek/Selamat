package com.tinsae.socialmediaplatform.admin.controller;

import com.tinsae.socialmediaplatform.admin.service.ModerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/moderation")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminModerationController {

    private final ModerationService moderationService;

    public AdminModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PatchMapping("/posts/{postId}/hide")
    public ResponseEntity<Void> hidePost(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        moderationService.hidePost(postId, jwt);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/comments/{commentId}/hide")
    public ResponseEntity<Void> hideComment(@PathVariable UUID commentId, @AuthenticationPrincipal Jwt jwt) {
        moderationService.hideComment(commentId, jwt);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/messages/{messageId}/delete")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId, @AuthenticationPrincipal Jwt jwt) {
        moderationService.deleteMessage(messageId, jwt);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/media/{mediaId}/hide")
    public ResponseEntity<Void> hideMedia(@PathVariable UUID mediaId, @AuthenticationPrincipal Jwt jwt) {
        moderationService.hideMedia(mediaId, jwt);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {
        moderationService.suspendUser(userId, jwt);
        return ResponseEntity.noContent().build();
    }
}
