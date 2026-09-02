package com.tinsae.socialmediaplatform.reaction.controller;

import com.tinsae.socialmediaplatform.reaction.dto.ReactionRequest;
import com.tinsae.socialmediaplatform.reaction.dto.ReactionResponse;
import com.tinsae.socialmediaplatform.reaction.service.ReactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PutMapping
    public ReactionResponse reactToPost(
            @PathVariable UUID postId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reactionService.reactToPost(postId, request, jwt);
    }

    @DeleteMapping
    public ResponseEntity<Void> removePostReaction(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        reactionService.removePostReaction(postId, jwt);
        return ResponseEntity.noContent().build();
    }
}
