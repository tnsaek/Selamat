package com.tinsae.socialmediaplatform.post.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.media.service.MediaService;
import com.tinsae.socialmediaplatform.post.dto.CreatePostRequest;
import com.tinsae.socialmediaplatform.post.dto.UpdatePostRequest;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private MediaService mediaService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PostService postService;

    @Test
    void createPostSavesPostForAuthenticatedUser() {
        User author = TestDataFactory.user("author");
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(UUID.randomUUID());
            return post;
        });
        when(mediaService.attachMediaToPost(any(), any(Post.class), any(User.class))).thenReturn(List.of());

        var response = postService.createPost(new CreatePostRequest("Hello", PostVisibility.PUBLIC, List.of()), jwt);

        assertThat(response.content()).isEqualTo("Hello");
        assertThat(response.author().id()).isEqualTo(author.getId());
    }

    @Test
    void createPostRejectsRequestWithoutContentOrMedia() {
        User author = TestDataFactory.user("author");
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);

        assertThatThrownBy(() -> postService.createPost(new CreatePostRequest("   ", PostVisibility.PUBLIC, List.of()), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Post must include content or media.");
    }

    @Test
    void getPostByIdReturnsVisiblePostWithMedia() {
        User author = TestDataFactory.user("author");
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(viewer);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(mediaService.findMediaForPost(post.getId())).thenReturn(List.of());

        var response = postService.getPostById(post.getId(), jwt);

        assertThat(response.id()).isEqualTo(post.getId());
        assertThat(response.content()).isEqualTo(post.getContent());
    }

    @Test
    void getPostByIdRejectsMissingPost() {
        User viewer = TestDataFactory.user("viewer");
        var jwt = TestDataFactory.jwt(viewer);
        UUID postId = UUID.randomUUID();
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(postId, jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void getPostByIdRejectsDeletedPost() {
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        var jwt = TestDataFactory.jwt(viewer);
        post.setStatus(PostStatus.DELETED);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPostById(post.getId(), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void getPostByIdRejectsHiddenPost() {
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        var jwt = TestDataFactory.jwt(viewer);
        post.setStatus(PostStatus.HIDDEN);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPostById(post.getId(), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void getPostByIdAllowsAuthorToViewPrivatePost() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        post.setVisibility(PostVisibility.PRIVATE);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(mediaService.findMediaForPost(post.getId())).thenReturn(List.of());

        var response = postService.getPostById(post.getId(), jwt);

        assertThat(response.id()).isEqualTo(post.getId());
    }

    @Test
    void getPostByIdAllowsAcceptedFollowerToViewFollowersOnlyPost() {
        User author = TestDataFactory.user("author");
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(author);
        post.setVisibility(PostVisibility.FOLLOWERS_ONLY);
        var jwt = TestDataFactory.jwt(viewer);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(followRepository.existsByFollowerIdAndFollowedIdAndStatus(
                viewer.getId(),
                author.getId(),
                FollowStatus.ACCEPTED
        )).thenReturn(true);
        when(mediaService.findMediaForPost(post.getId())).thenReturn(List.of());

        var response = postService.getPostById(post.getId(), jwt);

        assertThat(response.id()).isEqualTo(post.getId());
    }

    @Test
    void getPostByIdRejectsNonFollowerForFollowersOnlyPost() {
        User author = TestDataFactory.user("author");
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(author);
        post.setVisibility(PostVisibility.FOLLOWERS_ONLY);
        var jwt = TestDataFactory.jwt(viewer);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(followRepository.existsByFollowerIdAndFollowedIdAndStatus(
                viewer.getId(),
                author.getId(),
                FollowStatus.ACCEPTED
        )).thenReturn(false);

        assertThatThrownBy(() -> postService.getPostById(post.getId(), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void getPostByIdRejectsOtherUserForPrivatePost() {
        User author = TestDataFactory.user("author");
        User viewer = TestDataFactory.user("viewer");
        Post post = TestDataFactory.post(author);
        post.setVisibility(PostVisibility.PRIVATE);
        var jwt = TestDataFactory.jwt(viewer);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(viewer);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPostById(post.getId(), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void updatePostChangesContentAndVisibility() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(mediaService.findMediaForPost(post.getId())).thenReturn(List.of());

        var response = postService.updatePost(
                post.getId(),
                new UpdatePostRequest("Changed", PostVisibility.FOLLOWERS_ONLY),
                jwt
        );

        assertThat(response.content()).isEqualTo("Changed");
        assertThat(response.visibility()).isEqualTo(PostVisibility.FOLLOWERS_ONLY);
    }

    @Test
    void updatePostRejectsNoopRequest() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(post.getId(), new UpdatePostRequest(null, null), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("At least one post field must be provided.");
    }

    @Test
    void updatePostRejectsNonAuthor() {
        User author = TestDataFactory.user("author");
        User other = TestDataFactory.user("other");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(other);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(other);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(
                post.getId(),
                new UpdatePostRequest("Changed", null),
                jwt
        )).isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You can only modify your own posts.");
    }

    @Test
    void deletePostRejectsNonAuthor() {
        User author = TestDataFactory.user("author");
        User other = TestDataFactory.user("other");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(other);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(other);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(post.getId(), jwt))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You can only modify your own posts.");
    }

    @Test
    void deletePostSoftDeletesOwnPost() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        postService.deletePost(post.getId(), jwt);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
        verify(postRepository).save(post);
    }
}
