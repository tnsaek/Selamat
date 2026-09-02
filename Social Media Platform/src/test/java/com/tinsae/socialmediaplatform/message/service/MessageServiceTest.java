package com.tinsae.socialmediaplatform.message.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.message.dto.SendMessageRequest;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessageSavesMessage() {
        User sender = TestDataFactory.user("sender");
        User recipient = TestDataFactory.user("recipient");
        var jwt = TestDataFactory.jwt(sender);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(sender);
        when(userService.findActiveUserById(recipient.getId())).thenReturn(recipient);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            return message;
        });

        var response = messageService.sendMessage(new SendMessageRequest(recipient.getId(), "Hello"), jwt);

        assertThat(response.sender().id()).isEqualTo(sender.getId());
        assertThat(response.sender().username()).isEqualTo(sender.getUsername());
        assertThat(response.recipient().id()).isEqualTo(recipient.getId());
        assertThat(response.recipient().username()).isEqualTo(recipient.getUsername());
        assertThat(response.content()).isEqualTo("Hello");
    }

    @Test
    void sendMessageRejectsSelfMessage() {
        User sender = TestDataFactory.user("sender");
        var jwt = TestDataFactory.jwt(sender);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(sender);
        when(userService.findActiveUserById(sender.getId())).thenReturn(sender);

        assertThatThrownBy(() -> messageService.sendMessage(new SendMessageRequest(sender.getId(), "Hello"), jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("You cannot send a message to yourself.");
    }

    @Test
    void listMessagesReturnsCurrentUserMessages() {
        User sender = TestDataFactory.user("sender");
        User recipient = TestDataFactory.user("recipient");
        Message message = TestDataFactory.message(sender, recipient);
        var jwt = TestDataFactory.jwt(sender);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(sender);
        when(messageRepository.findUserMessages(any(), eq(MessageStatus.DELETED), any(), any()))
                .thenReturn(List.of(message));

        var response = messageService.listMessages(null, 10, jwt);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(message.getId());
    }

    @Test
    void getMessageByIdReturnsAccessibleMessage() {
        User sender = TestDataFactory.user("sender");
        User recipient = TestDataFactory.user("recipient");
        Message message = TestDataFactory.message(sender, recipient);
        var jwt = TestDataFactory.jwt(recipient);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(recipient);
        when(messageRepository.findAccessibleMessage(message.getId(), recipient.getId(), MessageStatus.DELETED))
                .thenReturn(Optional.of(message));

        var response = messageService.getMessageById(message.getId(), jwt);

        assertThat(response.id()).isEqualTo(message.getId());
        assertThat(response.sender().id()).isEqualTo(sender.getId());
        assertThat(response.recipient().id()).isEqualTo(recipient.getId());
    }

    @Test
    void getMessageByIdRejectsMissingOrInaccessibleMessage() {
        User user = TestDataFactory.user("user");
        UUID messageId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(messageRepository.findAccessibleMessage(eq(messageId), eq(user.getId()), eq(MessageStatus.DELETED)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.getMessageById(messageId, jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Message not found.");
    }

    @Test
    void markConversationAsReadMarksReceivedConversationMessages() {
        User currentUser = TestDataFactory.user("current");
        User participant = TestDataFactory.user("participant");
        var jwt = TestDataFactory.jwt(currentUser);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(currentUser);

        messageService.markConversationAsRead(participant.getId(), jwt);

        verify(messageRepository).markConversationMessagesRead(
                eq(currentUser.getId()),
                eq(participant.getId()),
                eq(MessageStatus.READ),
                eq(MessageStatus.DELETED),
                any()
        );
    }

    @Test
    void listMessagesUsesDefaultLimitWhenLimitIsNull() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(messageRepository.findUserMessages(any(), eq(MessageStatus.DELETED), any(), pageableCaptor.capture())).thenReturn(List.of());

        var response = messageService.listMessages(null, null, jwt);

        assertThat(response.items()).isEmpty();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(21);
    }

    @Test
    void listMessagesClampsLimitBelowOne() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(messageRepository.findUserMessages(any(), eq(MessageStatus.DELETED), any(), pageableCaptor.capture())).thenReturn(List.of());

        messageService.listMessages(null, 0, jwt);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void listMessagesClampsLimitAboveMax() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(messageRepository.findUserMessages(any(), eq(MessageStatus.DELETED), any(), pageableCaptor.capture())).thenReturn(List.of());

        messageService.listMessages(null, 101, jwt);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(101);
    }
}
