package com.tinsae.socialmediaplatform.admin.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.admin.service.ModerationService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminModerationControllerTest {

    @Mock
    private ModerationService moderationService;

    @InjectMocks
    private AdminModerationController adminModerationController;

    @Test
    void hidePostDelegatesToModerationService() {
        User moderator = TestDataFactory.user("moderator");
        var jwt = TestDataFactory.jwt(moderator);
        UUID postId = UUID.randomUUID();

        var response = adminModerationController.hidePost(postId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moderationService).hidePost(postId, jwt);
    }

    @Test
    void hideCommentDelegatesToModerationService() {
        User moderator = TestDataFactory.user("moderator");
        var jwt = TestDataFactory.jwt(moderator);
        UUID commentId = UUID.randomUUID();

        var response = adminModerationController.hideComment(commentId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moderationService).hideComment(commentId, jwt);
    }

    @Test
    void deleteMessageDelegatesToModerationService() {
        User moderator = TestDataFactory.user("moderator");
        var jwt = TestDataFactory.jwt(moderator);
        UUID messageId = UUID.randomUUID();

        var response = adminModerationController.deleteMessage(messageId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moderationService).deleteMessage(messageId, jwt);
    }

    @Test
    void hideMediaDelegatesToModerationService() {
        User moderator = TestDataFactory.user("moderator");
        var jwt = TestDataFactory.jwt(moderator);
        UUID mediaId = UUID.randomUUID();

        var response = adminModerationController.hideMedia(mediaId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moderationService).hideMedia(mediaId, jwt);
    }

    @Test
    void suspendUserDelegatesToModerationService() {
        User moderator = TestDataFactory.user("moderator");
        var jwt = TestDataFactory.jwt(moderator);
        UUID userId = UUID.randomUUID();

        var response = adminModerationController.suspendUser(userId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moderationService).suspendUser(userId, jwt);
    }
}
