package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.notification.entity.Notification;
import com.tinsae.socialmediaplatform.notification.repository.NotificationRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void recipientListsCountsAndMarksNotificationsRead() throws Exception {
        String authorUsername = uniqueUsername("notifyauthor");
        String viewerUsername = uniqueUsername("notifyviewer");
        String authorToken = extractAccessToken(signUp(authorUsername, authorUsername + "@example.com").body());
        String viewerToken = extractAccessToken(signUp(viewerUsername, viewerUsername + "@example.com").body());
        User author = userRepository.findByUsername(authorUsername).orElseThrow();
        User viewer = userRepository.findByUsername(viewerUsername).orElseThrow();

        String content = "Notification integration post " + UUID.randomUUID();
        HttpResponse<String> createPostResponse = postJson("/api/posts", """
                {
                  "content": "%s",
                  "visibility": "PUBLIC",
                  "mediaIds": []
                }
                """.formatted(content), authorToken);
        assertThat(createPostResponse.statusCode()).isEqualTo(201);

        Post post = postRepository.findAll()
                .stream()
                .filter(candidate -> content.equals(candidate.getContent()))
                .findFirst()
                .orElseThrow();

        HttpResponse<String> followResponse = postJson("/api/users/%s/follow".formatted(author.getId()), "{}", viewerToken);
        assertThat(followResponse.statusCode()).isEqualTo(201);

        HttpResponse<String> commentResponse = postJson("/api/posts/%s/comments".formatted(post.getId()), """
                {
                  "content": "This comment should notify the author."
                }
                """, viewerToken);
        assertThat(commentResponse.statusCode()).isEqualTo(201);

        List<Notification> unreadNotifications = notificationRepository.findByRecipientIdAndReadOrderByCreatedAtDesc(
                author.getId(),
                false,
                PageRequest.of(0, 10)
        );
        assertThat(unreadNotifications)
                .extracting(Notification::getNotificationType)
                .contains(NotificationType.FOLLOW, NotificationType.COMMENT);

        HttpResponse<String> unreadCountResponse = get("/api/notifications/unread-count", authorToken);

        assertThat(unreadCountResponse.statusCode()).isEqualTo(200);
        assertThat(unreadCountResponse.body()).contains("\"unreadCount\":2");

        HttpResponse<String> listResponse = get("/api/notifications?unreadOnly=true&limit=10", authorToken);

        assertThat(listResponse.statusCode()).isEqualTo(200);
        assertThat(listResponse.body())
                .contains("\"items\"")
                .contains("\"notificationType\":\"FOLLOW\"")
                .contains("\"notificationType\":\"COMMENT\"")
                .contains("\"targetType\":\"USER\"")
                .contains("\"targetType\":\"POST\"")
                .contains("\"targetId\":\"" + viewer.getId() + "\"")
                .contains("\"targetId\":\"" + post.getId() + "\"")
                .contains("\"read\":false");

        Notification notificationToRead = unreadNotifications.getFirst();
        HttpResponse<String> markOneReadResponse = patchJson(
                "/api/notifications/%s/read".formatted(notificationToRead.getId()),
                "{}",
                authorToken
        );

        assertThat(markOneReadResponse.statusCode()).isEqualTo(200);
        assertThat(markOneReadResponse.body())
                .contains("\"id\":\"" + notificationToRead.getId() + "\"")
                .contains("\"read\":true")
                .contains("\"readAt\"");
        assertThat(notificationRepository.findById(notificationToRead.getId()).orElseThrow().getRead()).isTrue();

        HttpResponse<String> updatedUnreadCountResponse = get("/api/notifications/unread-count", authorToken);
        assertThat(updatedUnreadCountResponse.statusCode()).isEqualTo(200);
        assertThat(updatedUnreadCountResponse.body()).contains("\"unreadCount\":1");

        HttpResponse<String> markAllReadResponse = patchJson("/api/notifications/read-all", "{}", authorToken);

        assertThat(markAllReadResponse.statusCode()).isEqualTo(200);
        assertThat(markAllReadResponse.body()).contains("\"unreadCount\":0");
        assertThat(notificationRepository.countByRecipientIdAndRead(author.getId(), false)).isZero();
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
