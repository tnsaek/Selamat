package com.tinsae.socialmediaplatform.notification.controller;

import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.notification.dto.NotificationCountResponse;
import com.tinsae.socialmediaplatform.notification.dto.NotificationResponse;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> listNotifications(
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return notificationService.listNotifications(unreadOnly, cursor, limit, jwt);
    }

    @GetMapping("/unread-count")
    public NotificationCountResponse getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.getUnreadCount(jwt);
    }

    @PatchMapping("/read-all")
    public NotificationCountResponse markAllNotificationsRead(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.markAllAsRead(jwt);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markNotificationRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return notificationService.markAsRead(notificationId, jwt);
    }
}
