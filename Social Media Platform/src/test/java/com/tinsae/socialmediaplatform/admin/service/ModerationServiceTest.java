package com.tinsae.socialmediaplatform.admin.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.admin.entity.ModerationAuditLog;
import com.tinsae.socialmediaplatform.admin.repository.ModerationAuditLogRepository;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ModerationAuditLogRepository moderationAuditLogRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ModerationService moderationService;

    @Test
    void hidePostChangesStatusToHidden() {
        User moderator = TestDataFactory.user("moderator");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        moderationService.hidePost(post.getId(), jwt);

        assertThat(post.getStatus()).isEqualTo(PostStatus.HIDDEN);
        verify(postRepository).save(post);
        verifyAudit(moderator, "HIDE_POST", ReportTargetType.POST, post.getId());
    }

    @Test
    void hidePostRejectsDeletedPost() {
        User moderator = TestDataFactory.user("moderator");
        Post post = TestDataFactory.post(TestDataFactory.user("author"));
        post.setStatus(PostStatus.DELETED);
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> moderationService.hidePost(post.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Deleted posts cannot be hidden.");
    }

    @Test
    void hidePostRejectsMissingPost() {
        User moderator = TestDataFactory.user("moderator");
        UUID postId = UUID.randomUUID();
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderationService.hidePost(postId, jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void hideCommentChangesStatusToHidden() {
        User moderator = TestDataFactory.user("moderator");
        Comment comment = TestDataFactory.comment(TestDataFactory.post(TestDataFactory.user("author")), TestDataFactory.user("commenter"));
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        moderationService.hideComment(comment.getId(), jwt);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.HIDDEN);
        verify(commentRepository).save(comment);
        verifyAudit(moderator, "HIDE_COMMENT", ReportTargetType.COMMENT, comment.getId());
    }

    @Test
    void hideCommentRejectsDeletedComment() {
        User moderator = TestDataFactory.user("moderator");
        Comment comment = TestDataFactory.comment(TestDataFactory.post(TestDataFactory.user("author")), TestDataFactory.user("commenter"));
        comment.setStatus(CommentStatus.DELETED);
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> moderationService.hideComment(comment.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Deleted comments cannot be hidden.");
    }

    @Test
    void deleteMessageChangesStatusToDeleted() {
        User moderator = TestDataFactory.user("moderator");
        Message message = TestDataFactory.message(TestDataFactory.user("sender"), TestDataFactory.user("recipient"));
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        moderationService.deleteMessage(message.getId(), jwt);

        assertThat(message.getStatus()).isEqualTo(MessageStatus.DELETED);
        verify(messageRepository).save(message);
        verifyAudit(moderator, "DELETE_MESSAGE", ReportTargetType.MESSAGE, message.getId());
    }

    @Test
    void hideMediaChangesStatusToHidden() {
        User moderator = TestDataFactory.user("moderator");
        Media media = TestDataFactory.media(TestDataFactory.user("uploader"));
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        moderationService.hideMedia(media.getId(), jwt);

        assertThat(media.getStatus()).isEqualTo(MediaStatus.HIDDEN);
        verify(mediaRepository).save(media);
        verifyAudit(moderator, "HIDE_MEDIA", ReportTargetType.MEDIA, media.getId());
    }

    @Test
    void hideMediaRejectsDeletedMedia() {
        User moderator = TestDataFactory.user("moderator");
        Media media = TestDataFactory.media(TestDataFactory.user("uploader"));
        media.setStatus(MediaStatus.DELETED);
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> moderationService.hideMedia(media.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Deleted media cannot be hidden.");
    }

    @Test
    void suspendUserChangesStatusToSuspended() {
        User moderator = TestDataFactory.user("moderator");
        User user = TestDataFactory.user("user");
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        moderationService.suspendUser(user.getId(), jwt);

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(userRepository).save(user);
        verifyAudit(moderator, "SUSPEND_USER", ReportTargetType.USER, user.getId());
    }

    @Test
    void suspendUserRejectsSelfSuspension() {
        User moderator = TestDataFactory.user("moderator");
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(userRepository.findById(moderator.getId())).thenReturn(Optional.of(moderator));

        assertThatThrownBy(() -> moderationService.suspendUser(moderator.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("You cannot suspend your own account.");
    }

    @Test
    void suspendUserRejectsAdminAccount() {
        User moderator = TestDataFactory.user("moderator");
        User admin = TestDataFactory.user("admin");
        TestDataFactory.userRole(admin, TestDataFactory.role("ADMIN"));
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> moderationService.suspendUser(admin.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Admin accounts cannot be suspended from this action.");
    }

    @Test
    void suspendUserRejectsDeletedUser() {
        User moderator = TestDataFactory.user("moderator");
        User user = TestDataFactory.user("user");
        user.setStatus(UserStatus.DELETED);
        Jwt jwt = TestDataFactory.jwt(moderator);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(moderator);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> moderationService.suspendUser(user.getId(), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Deleted users cannot be suspended.");
    }

    private void verifyAudit(User actor, String action, ReportTargetType targetType, UUID targetId) {
        ArgumentCaptor<ModerationAuditLog> auditCaptor = ArgumentCaptor.forClass(ModerationAuditLog.class);
        verify(moderationAuditLogRepository).save(auditCaptor.capture());
        ModerationAuditLog auditLog = auditCaptor.getValue();
        assertThat(auditLog.getActor()).isSameAs(actor);
        assertThat(auditLog.getAction()).isEqualTo(action);
        assertThat(auditLog.getTargetType()).isEqualTo(targetType);
        assertThat(auditLog.getTargetId()).isEqualTo(targetId);
    }
}
