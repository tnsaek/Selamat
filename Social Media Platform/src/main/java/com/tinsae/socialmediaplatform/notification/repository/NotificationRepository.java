package com.tinsae.socialmediaplatform.notification.repository;

import com.tinsae.socialmediaplatform.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIdAndReadOrderByCreatedAtDesc(UUID recipientId, Boolean read, Pageable pageable);

    long countByRecipientIdAndRead(UUID recipientId, Boolean read);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
            set n.read = true,
                n.readAt = :readAt
            where n.recipient.id = :recipientId
              and n.read = false
            """)
    int markAllUnreadAsRead(
            @Param("recipientId") UUID recipientId,
            @Param("readAt") Instant readAt
    );

    @Query("""
            select n from Notification n
            where n.recipient.id = :recipientId
              and (:unreadOnly = false or n.read = false)
              and (:cursor is null or n.createdAt < :cursor)
            order by n.createdAt desc
            """)
    List<Notification> findUserNotifications(
            @Param("recipientId") UUID recipientId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );
}
