package com.tinsae.socialmediaplatform.notification.service;

import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.NotificationMapper;
import com.tinsae.socialmediaplatform.common.util.CursorUtils;
import com.tinsae.socialmediaplatform.notification.dto.NotificationResponse;
import com.tinsae.socialmediaplatform.notification.dto.NotificationCountResponse;
import com.tinsae.socialmediaplatform.notification.entity.Notification;
import com.tinsae.socialmediaplatform.notification.repository.NotificationRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listNotifications(Boolean unreadOnly, String cursor, Integer limit, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Instant cursorInstant = CursorUtils.parseCursor(cursor);

        List<Notification> notifications = new ArrayList<>(notificationRepository.findUserNotifications(
                currentUser.getId(),
                Boolean.TRUE.equals(unreadOnly),
                cursorInstant,
                pageable
        ));
        String nextCursor = CursorUtils.trimAndNextCursor(notifications, pageSize, Notification::getCreatedAt);

        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationMapper::toResponse)
                .toList();

        return new PageResponse<>(responses, nextCursor);
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        long unreadCount = notificationRepository.countByRecipientIdAndRead(currentUser.getId(), false);
        return new NotificationCountResponse(unreadCount);
    }

    @Transactional
    public NotificationCountResponse markAllAsRead(Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        notificationRepository.markAllUnreadAsRead(currentUser.getId(), Instant.now());
        return new NotificationCountResponse(0);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only update your own notifications.");
        }

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }

        return NotificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void createNotification(
            User recipient,
            User actor,
            NotificationType notificationType,
            String title,
            String body,
            String targetType,
            UUID targetId
    ) {
        if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
