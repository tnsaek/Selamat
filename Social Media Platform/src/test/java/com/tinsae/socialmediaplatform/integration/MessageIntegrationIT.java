package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.notification.repository.NotificationRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void sendListReadConversationAndNotifyRecipient() throws Exception {
        String senderUsername = uniqueUsername("sender");
        String recipientUsername = uniqueUsername("recipient");
        String senderToken = extractAccessToken(signUp(senderUsername, senderUsername + "@example.com").body());
        String recipientToken = extractAccessToken(signUp(recipientUsername, recipientUsername + "@example.com").body());
        User sender = userRepository.findByUsername(senderUsername).orElseThrow();
        User recipient = userRepository.findByUsername(recipientUsername).orElseThrow();
        String content = "Integration message " + UUID.randomUUID();

        HttpResponse<String> sendResponse = postJson("/api/messages", """
                {
                  "recipientId": "%s",
                  "content": "%s"
                }
                """.formatted(recipient.getId(), content), senderToken);

        assertThat(sendResponse.statusCode()).isEqualTo(201);
        assertThat(sendResponse.body())
                .contains("\"content\":\"" + content + "\"")
                .contains("\"status\":\"SENT\"")
                .contains("\"username\":\"" + senderUsername + "\"")
                .contains("\"username\":\"" + recipientUsername + "\"");

        String messageId = extractStringField(sendResponse.body(), "id");
        Message savedMessage = messageRepository.findById(UUID.fromString(messageId)).orElseThrow();

        assertThat(savedMessage.getSender().getId()).isEqualTo(sender.getId());
        assertThat(savedMessage.getRecipient().getId()).isEqualTo(recipient.getId());
        assertThat(savedMessage.getStatus()).isEqualTo(MessageStatus.SENT);
        assertThat(savedMessage.getReadAt()).isNull();

        assertThat(notificationRepository.findAll())
                .anySatisfy(notification -> {
                    assertThat(notification.getRecipient().getId()).isEqualTo(recipient.getId());
                    assertThat(notification.getActor().getId()).isEqualTo(sender.getId());
                    assertThat(notification.getNotificationType()).isEqualTo(NotificationType.MESSAGE);
                    assertThat(notification.getTargetType()).isEqualTo("MESSAGE");
                    assertThat(notification.getTargetId()).isEqualTo(UUID.fromString(messageId));
                });

        HttpResponse<String> recipientListResponse = get("/api/messages?limit=10", recipientToken);

        assertThat(recipientListResponse.statusCode()).isEqualTo(200);
        assertThat(recipientListResponse.body())
                .contains("\"items\"")
                .contains("\"id\":\"" + messageId + "\"")
                .contains("\"content\":\"" + content + "\"");

        HttpResponse<String> detailResponse = get("/api/messages/" + messageId, recipientToken);

        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(detailResponse.body()).contains("\"id\":\"" + messageId + "\"");

        HttpResponse<String> markReadResponse = patchJson(
                "/api/messages/conversations/" + sender.getId() + "/read",
                "{}",
                recipientToken
        );

        assertThat(markReadResponse.statusCode()).isEqualTo(204);
        Message readMessage = messageRepository.findById(UUID.fromString(messageId)).orElseThrow();
        assertThat(readMessage.getStatus()).isEqualTo(MessageStatus.READ);
        assertThat(readMessage.getReadAt()).isNotNull();
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
