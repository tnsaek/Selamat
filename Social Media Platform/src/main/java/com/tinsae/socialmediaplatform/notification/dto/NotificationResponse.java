package com.tinsae.socialmediaplatform.notification.dto;

import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientId,
        UserSummaryResponse actor,
        NotificationType notificationType,
        String title,
        String body,
        String targetType,
        UUID targetId,
        Boolean read,
        Instant createdAt,
        Instant readAt
) {
}
