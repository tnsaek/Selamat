package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.notification.dto.NotificationResponse;
import com.tinsae.socialmediaplatform.notification.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient().getId(),
                notification.getActor() != null ? UserMapper.toSummary(notification.getActor()) : null,
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
