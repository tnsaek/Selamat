package com.tinsae.socialmediaplatform.post.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.post.dto.CreatePostRequest;
import com.tinsae.socialmediaplatform.post.dto.PostResponse;
import com.tinsae.socialmediaplatform.post.dto.UpdatePostRequest;
import com.tinsae.socialmediaplatform.post.service.PostService;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    @Test
    void createPostReturnsCreatedResponse() {
        User author = TestDataFactory.user("author");
        var jwt = TestDataFactory.jwt(author);
        CreatePostRequest request = new CreatePostRequest("Hello", PostVisibility.PUBLIC, List.of());
        PostResponse post = postResponse(author.getId());
        when(postService.createPost(request, jwt)).thenReturn(post);

        var response = postController.createPost(request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(post);
        verify(postService).createPost(request, jwt);
    }

    @Test
    void getPostByIdDelegatesToPostService() {
        User viewer = TestDataFactory.user("viewer");
        var jwt = TestDataFactory.jwt(viewer);
        PostResponse post = postResponse(UUID.randomUUID());
        when(postService.getPostById(post.id(), jwt)).thenReturn(post);

        PostResponse response = postController.getPostById(post.id(), jwt);

        assertThat(response).isSameAs(post);
        verify(postService).getPostById(post.id(), jwt);
    }

    @Test
    void updatePostDelegatesToPostService() {
        User author = TestDataFactory.user("author");
        var jwt = TestDataFactory.jwt(author);
        UUID postId = UUID.randomUUID();
        UpdatePostRequest request = new UpdatePostRequest("Changed", PostVisibility.PRIVATE);
        PostResponse post = postResponse(author.getId());
        when(postService.updatePost(postId, request, jwt)).thenReturn(post);

        PostResponse response = postController.updatePost(postId, request, jwt);

        assertThat(response).isSameAs(post);
        verify(postService).updatePost(postId, request, jwt);
    }

    @Test
    void deletePostReturnsNoContent() {
        User author = TestDataFactory.user("author");
        var jwt = TestDataFactory.jwt(author);
        UUID postId = UUID.randomUUID();

        var response = postController.deletePost(postId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(postService).deletePost(postId, jwt);
    }

    private PostResponse postResponse(UUID authorId) {
        UserSummaryResponse author = new UserSummaryResponse(authorId, "author", "Author", null);
        return new PostResponse(
                UUID.randomUUID(),
                author,
                "Hello",
                PostVisibility.PUBLIC,
                PostStatus.PUBLISHED,
                List.of(),
                0L,
                0L,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
