package com.tinsae.socialmediaplatform.comment.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.comment.dto.CommentResponse;
import com.tinsae.socialmediaplatform.comment.dto.CreateCommentRequest;
import com.tinsae.socialmediaplatform.comment.service.CommentService;
import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
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
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    @Test
    void listPostCommentsDelegatesToCommentService() {
        UUID postId = UUID.randomUUID();
        CommentResponse comment = commentResponse(postId, null);
        PageResponse<CommentResponse> page = new PageResponse<>(List.of(comment), null);
        when(commentService.listPostComments(postId, "cursor", 10)).thenReturn(page);

        PageResponse<CommentResponse> response = commentController.listPostComments(postId, "cursor", 10);

        assertThat(response).isSameAs(page);
        assertThat(response.items()).containsExactly(comment);
        verify(commentService).listPostComments(postId, "cursor", 10);
    }

    @Test
    void createCommentReturnsCreatedResponse() {
        User author = TestDataFactory.user("author");
        var jwt = TestDataFactory.jwt(author);
        UUID postId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest("Nice post", null);
        CommentResponse comment = commentResponse(postId, null);
        when(commentService.createComment(postId, request, jwt)).thenReturn(comment);

        var response = commentController.createComment(postId, request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(comment);
        verify(commentService).createComment(postId, request, jwt);
    }

    private CommentResponse commentResponse(UUID postId, UUID parentCommentId) {
        UserSummaryResponse author = new UserSummaryResponse(UUID.randomUUID(), "author", "Author", null);
        return new CommentResponse(
                UUID.randomUUID(),
                postId,
                parentCommentId,
                author,
                "Nice post",
                CommentStatus.VISIBLE,
                0L,
                Instant.now()
        );
    }
}
