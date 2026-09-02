package com.tinsae.socialmediaplatform.post.service;

import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.MediaMapper;
import com.tinsae.socialmediaplatform.common.mapper.PostMapper;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.service.MediaService;
import com.tinsae.socialmediaplatform.post.dto.CreatePostRequest;
import com.tinsae.socialmediaplatform.post.dto.PostResponse;
import com.tinsae.socialmediaplatform.post.dto.UpdatePostRequest;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final MediaService mediaService;
    private final UserService userService;

    public PostService(
            PostRepository postRepository,
            FollowRepository followRepository,
            MediaService mediaService,
            UserService userService
    ) {
        this.postRepository = postRepository;
        this.followRepository = followRepository;
        this.mediaService = mediaService;
        this.userService = userService;
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request, Jwt jwt) {
        User author = userService.getAuthenticatedUser(jwt);
        validatePostHasContentOrMedia(request.content(), request.mediaIds());

        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content());
        post.setVisibility(request.visibility());
        post.setStatus(PostStatus.PUBLISHED);

        Post savedPost = postRepository.save(post);
        List<Media> media = mediaService.attachMediaToPost(request.mediaIds(), savedPost, author);

        return toResponse(savedPost, media);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(UUID postId, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        Post post = findVisiblePost(postId);
        requireCanView(post, currentUser);
        return toResponse(post, mediaService.findMediaForPost(post.getId()));
    }

    @Transactional
    public PostResponse updatePost(UUID postId, UpdatePostRequest request, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        Post post = findVisiblePost(postId);
        requireAuthor(post, currentUser);

        if (request.content() == null && request.visibility() == null) {
            throw new BusinessRuleException("At least one post field must be provided.");
        }

        if (request.content() != null) {
            if (request.content().isBlank() && mediaService.findMediaForPost(post.getId()).isEmpty()) {
                throw new BusinessRuleException("Post must include content or media.");
            }
            post.setContent(request.content());
        }
        if (request.visibility() != null) {
            post.setVisibility(request.visibility());
        }

        Post savedPost = postRepository.save(post);
        return toResponse(savedPost, mediaService.findMediaForPost(savedPost.getId()));
    }

    @Transactional
    public void deletePost(UUID postId, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        Post post = findVisiblePost(postId);
        requireAuthor(post, currentUser);

        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    private Post findVisiblePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Post not found.");
        }

        return post;
    }

    private void requireCanView(Post post, User currentUser) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Post not found.");
        }

        if (post.getAuthor().getId().equals(currentUser.getId())) {
            return;
        }

        if (post.getVisibility() == PostVisibility.PUBLIC) {
            return;
        }

        if (post.getVisibility() == PostVisibility.FOLLOWERS_ONLY
                && followRepository.existsByFollowerIdAndFollowedIdAndStatus(
                currentUser.getId(),
                post.getAuthor().getId(),
                FollowStatus.ACCEPTED
        )) {
            return;
        }

        throw new ResourceNotFoundException("Post not found.");
    }

    private void requireAuthor(Post post, User currentUser) {
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only modify your own posts.");
        }
    }

    private void validatePostHasContentOrMedia(String content, List<UUID> mediaIds) {
        boolean hasContent = content != null && !content.isBlank();
        boolean hasMedia = mediaIds != null && !mediaIds.isEmpty();

        if (!hasContent && !hasMedia) {
            throw new BusinessRuleException("Post must include content or media.");
        }
    }

    private PostResponse toResponse(Post post, List<Media> media) {
        List<MediaResponse> mediaResponses = media.stream()
                .map(MediaMapper::toResponse)
                .toList();

        return PostMapper.toResponse(post, mediaResponses, null);
    }
}
