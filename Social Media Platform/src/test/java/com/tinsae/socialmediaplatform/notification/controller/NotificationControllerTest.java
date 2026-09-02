package com.tinsae.socialmediaplatform.notification.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.notification.dto.NotificationCountResponse;
import com.tinsae.socialmediaplatform.notification.dto.NotificationResponse;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void listNotificationsDelegatesToNotificationService() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        NotificationResponse notification = notificationResponse(recipient.getId());
        PageResponse<NotificationResponse> page = new PageResponse<>(List.of(notification), null);
        when(notificationService.listNotifications(true, "cursor", 10, jwt)).thenReturn(page);

        PageResponse<NotificationResponse> response = notificationController.listNotifications(true, "cursor", 10, jwt);

        assertThat(response).isSameAs(page);
        assertThat(response.items()).containsExactly(notification);
        verify(notificationService).listNotifications(true, "cursor", 10, jwt);
    }

    @Test
    void markNotificationReadDelegatesToNotificationService() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        UUID notificationId = UUID.randomUUID();
        NotificationResponse notification = notificationResponse(recipient.getId());
        when(notificationService.markAsRead(notificationId, jwt)).thenReturn(notification);

        NotificationResponse response = notificationController.markNotificationRead(notificationId, jwt);

        assertThat(response).isSameAs(notification);
        verify(notificationService).markAsRead(notificationId, jwt);
    }

    @Test
    void getUnreadCountDelegatesToNotificationService() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        NotificationCountResponse count = new NotificationCountResponse(3);
        when(notificationService.getUnreadCount(jwt)).thenReturn(count);

        NotificationCountResponse response = notificationController.getUnreadCount(jwt);

        assertThat(response).isSameAs(count);
        verify(notificationService).getUnreadCount(jwt);
    }

    @Test
    void markAllNotificationsReadDelegatesToNotificationService() {
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(recipient);
        NotificationCountResponse count = new NotificationCountResponse(0);
        when(notificationService.markAllAsRead(jwt)).thenReturn(count);

        NotificationCountResponse response = notificationController.markAllNotificationsRead(jwt);

        assertThat(response).isSameAs(count);
        verify(notificationService).markAllAsRead(jwt);
    }

    private NotificationResponse notificationResponse(UUID recipientId) {
        UserSummaryResponse actor = new UserSummaryResponse(UUID.randomUUID(), "actor", "Actor", null);
        return new NotificationResponse(
                UUID.randomUUID(),
                recipientId,
                actor,
                NotificationType.MESSAGE,
                "Title",
                "Body",
                "MESSAGE",
                UUID.randomUUID(),
                false,
                Instant.now(),
                null
        );
    }
}
