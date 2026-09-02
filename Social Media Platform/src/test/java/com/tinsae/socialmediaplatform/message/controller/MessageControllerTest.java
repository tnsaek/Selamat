package com.tinsae.socialmediaplatform.message.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.message.dto.MessageResponse;
import com.tinsae.socialmediaplatform.message.dto.SendMessageRequest;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.message.service.MessageService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    @Test
    void listMessagesDelegatesToMessageService() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        MessageResponse message = messageResponse(user.getId(), UUID.randomUUID());
        PageResponse<MessageResponse> page = new PageResponse<>(List.of(message), null);
        when(messageService.listMessages("cursor", 10, jwt)).thenReturn(page);

        PageResponse<MessageResponse> response = messageController.listMessages("cursor", 10, jwt);

        assertThat(response).isSameAs(page);
        assertThat(response.items()).containsExactly(message);
        verify(messageService).listMessages("cursor", 10, jwt);
    }

    @Test
    void sendMessageReturnsCreatedResponse() {
        User sender = TestDataFactory.user("sender");
        var jwt = TestDataFactory.jwt(sender);
        UUID recipientId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest(recipientId, "Hello");
        MessageResponse message = messageResponse(sender.getId(), recipientId);
        when(messageService.sendMessage(request, jwt)).thenReturn(message);

        var response = messageController.sendMessage(request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(message);
        verify(messageService).sendMessage(request, jwt);
    }

    @Test
    void getMessageByIdDelegatesToMessageService() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        UUID messageId = UUID.randomUUID();
        MessageResponse message = messageResponse(user.getId(), UUID.randomUUID());
        when(messageService.getMessageById(messageId, jwt)).thenReturn(message);

        MessageResponse response = messageController.getMessageById(messageId, jwt);

        assertThat(response).isSameAs(message);
        verify(messageService).getMessageById(messageId, jwt);
    }

    @Test
    void markConversationAsReadDelegatesToMessageService() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        UUID participantId = UUID.randomUUID();

        var response = messageController.markConversationAsRead(participantId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(messageService).markConversationAsRead(participantId, jwt);
    }

    private MessageResponse messageResponse(UUID senderId, UUID recipientId) {
        return new MessageResponse(
                UUID.randomUUID(),
                new UserSummaryResponse(senderId, "sender", null, null),
                new UserSummaryResponse(recipientId, "recipient", null, null),
                "Hello",
                MessageStatus.SENT,
                Instant.now(),
                null,
                null
        );
    }
}
