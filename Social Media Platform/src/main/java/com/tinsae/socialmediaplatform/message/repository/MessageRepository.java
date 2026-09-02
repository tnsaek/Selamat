package com.tinsae.socialmediaplatform.message.repository;

import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySenderIdOrRecipientIdOrderBySentAtDesc(UUID senderId, UUID recipientId, Pageable pageable);

    List<Message> findBySenderIdAndRecipientIdOrRecipientIdAndSenderIdOrderBySentAtAsc(
            UUID senderId,
            UUID recipientId,
            UUID reverseSenderId,
            UUID reverseRecipientId,
            Pageable pageable
    );

    @Query("""
            select m from Message m
            join fetch m.sender sender
            left join fetch sender.profile
            join fetch m.recipient recipient
            left join fetch recipient.profile
            where m.id = :messageId
              and m.status <> :deletedStatus
              and (m.sender.id = :userId or m.recipient.id = :userId)
            """)
    Optional<Message> findAccessibleMessage(
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId,
            @Param("deletedStatus") MessageStatus deletedStatus
    );

    @Query("""
            select m from Message m
            join fetch m.sender sender
            left join fetch sender.profile
            join fetch m.recipient recipient
            left join fetch recipient.profile
            where (m.sender.id = :userId or m.recipient.id = :userId)
              and m.status <> :deletedStatus
              and (:cursor is null or m.sentAt < :cursor)
            order by m.sentAt desc
            """)
    List<Message> findUserMessages(
            @Param("userId") UUID userId,
            @Param("deletedStatus") MessageStatus deletedStatus,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );

    @Modifying
    @Query("""
            update Message m
            set m.status = :readStatus,
                m.readAt = :readAt
            where m.sender.id = :participantId
              and m.recipient.id = :currentUserId
              and m.status <> :readStatus
              and m.status <> :deletedStatus
            """)
    int markConversationMessagesRead(
            @Param("currentUserId") UUID currentUserId,
            @Param("participantId") UUID participantId,
            @Param("readStatus") MessageStatus readStatus,
            @Param("deletedStatus") MessageStatus deletedStatus,
            @Param("readAt") Instant readAt
    );
}
