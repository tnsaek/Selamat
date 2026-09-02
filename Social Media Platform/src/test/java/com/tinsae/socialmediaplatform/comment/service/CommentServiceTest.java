package com.tinsae.socialmediaplatform.comment.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.comment.dto.CreateCommentRequest;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createCommentSavesCommentAndIncrementsPostCount() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            return comment;
        });

        var response = commentService.createComment(post.getId(), new CreateCommentRequest("Nice post", null), jwt);

        assertThat(response.content()).isEqualTo("Nice post");
        assertThat(post.getCommentCount()).isEqualTo(1L);
        verify(postRepository).save(post);
    }

    @Test
    void createReplyRejectsParentFromAnotherPost() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        Post otherPost = TestDataFactory.post(author);
        Comment parent = TestDataFactory.comment(otherPost, author);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(
                post.getId(),
                new CreateCommentRequest("Reply", parent.getId()),
                jwt
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Parent comment must belong to the same post.");
    }

    @Test
    void createReplyAcceptsParentFromSamePost() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        Comment parent = TestDataFactory.comment(post, author);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            return comment;
        });

        var response = commentService.createComment(
                post.getId(),
                new CreateCommentRequest("Reply", parent.getId()),
                jwt
        );

        assertThat(response.parentCommentId()).isEqualTo(parent.getId());
        assertThat(post.getCommentCount()).isEqualTo(1L);
        verify(postRepository).save(post);
    }

    @Test
    void createReplyRejectsHiddenParentComment() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        Comment parent = TestDataFactory.comment(post, author);
        parent.setStatus(CommentStatus.DELETED);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(
                post.getId(),
                new CreateCommentRequest("Reply", parent.getId()),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment not found.");
    }

    @Test
    void createReplyRejectsMissingParentComment() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        UUID parentCommentId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(
                post.getId(),
                new CreateCommentRequest("Reply", parentCommentId),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment not found.");
    }

    @Test
    void createCommentRejectsDeletedPost() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        post.setStatus(PostStatus.DELETED);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(
                post.getId(),
                new CreateCommentRequest("Comment", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void listPostCommentsRejectsMissingPost() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.listPostComments(postId, null, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void createCommentRejectsHiddenPost() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        post.setStatus(PostStatus.HIDDEN);
        var jwt = TestDataFactory.jwt(author);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(
                post.getId(),
                new CreateCommentRequest("Comment", null),
                jwt
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void listPostCommentsReturnsVisibleComments() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        Comment comment = TestDataFactory.comment(post, author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findVisiblePostComments(
                any(UUID.class),
                org.mockito.ArgumentMatchers.eq(CommentStatus.VISIBLE),
                any(),
                any()
        )).thenReturn(java.util.List.of(comment));

        var response = commentService.listPostComments(post.getId(), null, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(comment.getId());
    }

    @Test
    void listPostCommentsUsesDefaultLimitWhenLimitIsNull() {
        User author = TestDataFactory.user("author");
        Post post = TestDataFactory.post(author);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findVisiblePostComments(
                any(UUID.class),
                org.mockito.ArgumentMatchers.eq(CommentStatus.VISIBLE),
                any(),
                any()
        )).thenReturn(java.util.List.of());

        var response = commentService.listPostComments(post.getId(), null, null);

        assertThat(response.items()).isEmpty();
    }
}
