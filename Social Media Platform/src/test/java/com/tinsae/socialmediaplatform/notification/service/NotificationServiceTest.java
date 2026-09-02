package com.tinsae.socialmediaplatform.notification.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.notification.entity.Notification;
import com.tinsae.socialmediaplatform.notification.repository.NotificationRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void listNotificationsCanFilterUnreadOnly() {
        User recipient = TestDataFactory.user("recipient");
        Notification notification = TestDataFactory.notification(recipient, TestDataFactory.user("actor"));
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findUserNotifications(eq(recipient.getId()), eq(true), any(), any()))
                .thenReturn(List.of(notification));

        var response = notificationService.listNotifications(true, null, 10, jwt);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().read()).isFalse();
    }

    @Test
    void listNotificationsPassesParsedCursorAndReturnsNextCursor() {
        User recipient = TestDataFactory.user("recipient");
        Notification first = TestDataFactory.notification(recipient, TestDataFactory.user("actor-one"));
        Notification extra = TestDataFactory.notification(recipient, TestDataFactory.user("actor-two"));
        first.setCreatedAt(Instant.parse("2026-07-16T10:15:30Z"));
        extra.setCreatedAt(Instant.parse("2026-07-15T10:15:30Z"));
        var jwt = TestDataFactory.jwt(recipient);
        ArgumentCaptor<Instant> cursorCaptor = ArgumentCaptor.forClass(Instant.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findUserNotifications(
                eq(recipient.getId()),
                eq(true),
                cursorCaptor.capture(),
                any()
        )).thenReturn(List.of(first, extra));

        var response = notificationService.listNotifications(
                true,
                "2026-07-17T10:15:30Z",
                1,
                jwt
        );

        assertThat(cursorCaptor.getValue()).isEqualTo(Instant.parse("2026-07-17T10:15:30Z"));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(first.getId());
        assertThat(response.nextCursor()).isEqualTo("2026-07-16T10:15:30Z");
    }

    @Test
    void listNotificationsRejectsInvalidCursor() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);

        assertThatThrownBy(() -> notificationService.listNotifications(false, "not-a-date", 10, jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Invalid cursor format. Use ISO-8601 format.");
    }

    @Test
    void listNotificationsReturnsAllWhenUnreadOnlyIsFalse() {
        User recipient = TestDataFactory.user("recipient");
        Notification notification = TestDataFactory.notification(recipient, TestDataFactory.user("actor"));
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findUserNotifications(eq(recipient.getId()), eq(false), any(), any()))
                .thenReturn(List.of(notification));

        var response = notificationService.listNotifications(false, null, 10, jwt);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(notification.getId());
    }

    @Test
    void listNotificationsUsesDefaultLimitWhenLimitIsNull() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findUserNotifications(eq(recipient.getId()), eq(false), any(), pageableCaptor.capture()))
                .thenReturn(List.of());

        var response = notificationService.listNotifications(null, null, null, jwt);

        assertThat(response.items()).isEmpty();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    void listNotificationsClampsLimitBelowOne() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findUserNotifications(eq(recipient.getId()), eq(false), any(), pageableCaptor.capture()))
                .thenReturn(List.of());

        notificationService.listNotifications(false, null, 0, jwt);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void listNotificationsClampsLimitAboveMax() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findUserNotifications(eq(recipient.getId()), eq(false), any(), pageableCaptor.capture()))
                .thenReturn(List.of());

        notificationService.listNotifications(false, null, 101, jwt);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(101);
    }

    @Test
    void getUnreadCountReturnsUnreadNotificationCount() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.countByRecipientIdAndRead(recipient.getId(), false)).thenReturn(4L);

        var response = notificationService.getUnreadCount(jwt);

        assertThat(response.unreadCount()).isEqualTo(4L);
    }

    @Test
    void markAllAsReadMarksCurrentUsersUnreadNotificationsRead() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);

        var response = notificationService.markAllAsRead(jwt);

        assertThat(response.unreadCount()).isZero();
        verify(notificationRepository).markAllUnreadAsRead(eq(recipient.getId()), any(Instant.class));
    }

    @Test
    void markAsReadUpdatesUnreadNotification() {
        User recipient = TestDataFactory.user("recipient");
        Notification notification = TestDataFactory.notification(recipient, TestDataFactory.user("actor"));
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        var response = notificationService.markAsRead(notification.getId(), jwt);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void markAsReadLeavesAlreadyReadNotificationReadAtUnchanged() {
        User recipient = TestDataFactory.user("recipient");
        Notification notification = TestDataFactory.notification(recipient, TestDataFactory.user("actor"));
        notification.setRead(true);
        notification.setReadAt(java.time.Instant.now().minusSeconds(60));
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        var response = notificationService.markAsRead(notification.getId(), jwt);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isEqualTo(notification.getReadAt());
    }

    @Test
    void markAsReadRejectsMissingNotification() {
        User recipient = TestDataFactory.user("recipient");
        UUID notificationId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notification not found.");
    }

    @Test
    void markAsReadRejectsOtherUsersNotification() {
        User recipient = TestDataFactory.user("recipient");
        User otherUser = TestDataFactory.user("other");
        Notification notification = TestDataFactory.notification(recipient, TestDataFactory.user("actor"));
        var jwt = TestDataFactory.jwt(otherUser);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(otherUser);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notification.getId(), jwt))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You can only update your own notifications.");
    }

    @Test
    void createNotificationSavesNotification() {
        User recipient = TestDataFactory.user("recipient");
        User actor = TestDataFactory.user("actor");
        UUID targetId = UUID.randomUUID();
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createNotification(
                recipient,
                actor,
                NotificationType.COMMENT,
                "Comment",
                "Actor commented",
                "POST",
                targetId
        );

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getActor()).isSameAs(actor);
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.COMMENT);
        assertThat(notification.getTitle()).isEqualTo("Comment");
        assertThat(notification.getBody()).isEqualTo("Actor commented");
        assertThat(notification.getTargetType()).isEqualTo("POST");
        assertThat(notification.getTargetId()).isEqualTo(targetId);
        assertThat(notification.getRead()).isFalse();
    }

    @Test
    void createNotificationDoesNothingWhenRecipientIsNull() {
        notificationService.createNotification(
                null,
                TestDataFactory.user("actor"),
                NotificationType.COMMENT,
                "Title",
                "Body",
                "POST",
                UUID.randomUUID()
        );

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void createNotificationDoesNothingWhenActorIsNull() {
        notificationService.createNotification(
                TestDataFactory.user("recipient"),
                null,
                NotificationType.COMMENT,
                "Title",
                "Body",
                "POST",
                UUID.randomUUID()
        );

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void createNotificationDoesNothingWhenRecipientIsActor() {
        User user = TestDataFactory.user("same");

        notificationService.createNotification(
                user,
                user,
                NotificationType.COMMENT,
                "Title",
                "Body",
                "POST",
                UUID.randomUUID()
        );

        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
