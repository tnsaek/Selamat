package com.tinsae.socialmediaplatform.message.service;

import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.mapper.MessageMapper;
import com.tinsae.socialmediaplatform.common.util.CursorUtils;
import com.tinsae.socialmediaplatform.message.dto.MessageResponse;
import com.tinsae.socialmediaplatform.message.dto.SendMessageRequest;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
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
public class MessageService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final MessageRepository messageRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    public MessageService(
            MessageRepository messageRepository,
            NotificationService notificationService,
            UserService userService
    ) {
        this.messageRepository = messageRepository;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, Jwt jwt) {
        User sender = userService.getAuthenticatedUser(jwt);
        User recipient = userService.findActiveUserById(request.recipientId());

        if (sender.getId().equals(recipient.getId())) {
            throw new BusinessRuleException("You cannot send a message to yourself.");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(request.content());
        message.setStatus(MessageStatus.SENT);

        Message savedMessage = messageRepository.save(message);
        notificationService.createNotification(
                recipient,
                sender,
                NotificationType.MESSAGE,
                "New message",
                sender.getUsername() + " sent you a message.",
                "MESSAGE",
                savedMessage.getId()
        );
        return MessageMapper.toResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(String cursor, Integer limit, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Instant cursorInstant = CursorUtils.parseCursor(cursor);

        List<Message> userMessages = new ArrayList<>(messageRepository
                .findUserMessages(currentUser.getId(), MessageStatus.DELETED, cursorInstant, pageable));
        String nextCursor = CursorUtils.trimAndNextCursor(userMessages, pageSize, Message::getSentAt);

        List<MessageResponse> messages = userMessages
                .stream()
                .map(MessageMapper::toResponse)
                .toList();

        return new PageResponse<>(messages, nextCursor);
    }

    @Transactional(readOnly = true)
    public MessageResponse getMessageById(UUID messageId, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        Message message = messageRepository.findAccessibleMessage(messageId, currentUser.getId(), MessageStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));

        return MessageMapper.toResponse(message);
    }

    @Transactional
    public void markConversationAsRead(UUID participantId, Jwt jwt) {
        User currentUser = userService.getAuthenticatedUser(jwt);
        messageRepository.markConversationMessagesRead(
                currentUser.getId(),
                participantId,
                MessageStatus.READ,
                MessageStatus.DELETED,
                Instant.now()
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
