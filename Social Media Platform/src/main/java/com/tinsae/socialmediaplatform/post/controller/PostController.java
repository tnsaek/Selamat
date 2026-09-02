package com.tinsae.socialmediaplatform.post.controller;

import com.tinsae.socialmediaplatform.post.dto.CreatePostRequest;
import com.tinsae.socialmediaplatform.post.dto.PostResponse;
import com.tinsae.socialmediaplatform.post.dto.UpdatePostRequest;
import com.tinsae.socialmediaplatform.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request, jwt));
    }

    @GetMapping("/{postId}")
    public PostResponse getPostById(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        return postService.getPostById(postId, jwt);
    }

    @PatchMapping("/{postId}")
    public PostResponse updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return postService.updatePost(postId, request, jwt);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID postId, @AuthenticationPrincipal Jwt jwt) {
        postService.deletePost(postId, jwt);
        return ResponseEntity.noContent().build();
    }
}
