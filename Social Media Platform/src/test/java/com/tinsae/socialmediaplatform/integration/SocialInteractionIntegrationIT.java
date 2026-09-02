package com.tinsae.socialmediaplatform.integration;

import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.follow.repository.FollowRepository;
import com.tinsae.socialmediaplatform.notification.entity.Notification;
import com.tinsae.socialmediaplatform.notification.repository.NotificationRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.reaction.repository.ReactionRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SocialInteractionIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void followCommentAndReactionPersistStateAndNotifyPostAuthor() throws Exception {
        String authorUsername = uniqueUsername("author");
        String viewerUsername = uniqueUsername("viewer");
        String authorToken = extractAccessToken(signUp(authorUsername, authorUsername + "@example.com").body());
        String viewerToken = extractAccessToken(signUp(viewerUsername, viewerUsername + "@example.com").body());
        User author = userRepository.findByUsername(authorUsername).orElseThrow();
        User viewer = userRepository.findByUsername(viewerUsername).orElseThrow();

        HttpResponse<String> createPostResponse = postJson("/api/posts", """
                {
                  "content": "Post for social interaction integration",
                  "visibility": "PUBLIC",
                  "mediaIds": []
                }
                """, authorToken);
        String postId = extractStringField(createPostResponse.body(), "id");

        HttpResponse<String> followResponse = postJson("/api/users/" + author.getId() + "/follow", "{}", viewerToken);

        assertThat(followResponse.statusCode()).isEqualTo(201);
        assertThat(followRepository.existsByFollowerIdAndFollowedIdAndStatus(
                viewer.getId(),
                author.getId(),
                FollowStatus.ACCEPTED
        )).isTrue();

        HttpResponse<String> followStatusResponse = get("/api/users/" + author.getId() + "/follow", viewerToken);

        assertThat(followStatusResponse.statusCode()).isEqualTo(200);
        assertThat(followStatusResponse.body()).contains("\"following\":true");

        HttpResponse<String> commentResponse = postJson("/api/posts/" + postId + "/comments", """
                {
                  "content": "This is an integration comment.",
                  "parentCommentId": null
                }
                """, viewerToken);

        assertThat(commentResponse.statusCode()).isEqualTo(201);
        assertThat(commentRepository.findAll())
                .anySatisfy(comment -> {
                    assertThat(comment.getPost().getId()).isEqualTo(UUID.fromString(postId));
                    assertThat(comment.getAuthor().getId()).isEqualTo(viewer.getId());
                    assertThat(comment.getContent()).isEqualTo("This is an integration comment.");
                });

        HttpResponse<String> reactionResponse = putJson("/api/posts/" + postId + "/reactions", """
                {
                  "reactionType": "LIKE"
                }
                """, viewerToken);

        assertThat(reactionResponse.statusCode()).isEqualTo(200);
        assertThat(reactionRepository.findByUserIdAndPostId(viewer.getId(), UUID.fromString(postId)))
                .hasValueSatisfying(reaction -> assertThat(reaction.getReactionType()).isEqualTo(ReactionType.LIKE));

        Post updatedPost = postRepository.findById(UUID.fromString(postId)).orElseThrow();
        assertThat(updatedPost.getCommentCount()).isEqualTo(1);
        assertThat(updatedPost.getReactionCount()).isEqualTo(1);

        List<Notification> authorNotifications = notificationRepository.findAll()
                .stream()
                .filter(notification -> notification.getRecipient().getId().equals(author.getId()))
                .toList();

        assertThat(authorNotifications)
                .extracting(Notification::getNotificationType)
                .contains(NotificationType.FOLLOW, NotificationType.COMMENT, NotificationType.REACTION);
        assertThat(notificationRepository.countByRecipientIdAndRead(author.getId(), false)).isEqualTo(3);

        HttpResponse<String> unreadCountResponse = get("/api/notifications/unread-count", authorToken);

        assertThat(unreadCountResponse.statusCode()).isEqualTo(200);
        assertThat(unreadCountResponse.body()).contains("\"unreadCount\":3");

        HttpResponse<String> removeReactionResponse = delete("/api/posts/" + postId + "/reactions", viewerToken);

        assertThat(removeReactionResponse.statusCode()).isEqualTo(204);
        assertThat(reactionRepository.findByUserIdAndPostId(viewer.getId(), UUID.fromString(postId))).isEmpty();
        assertThat(postRepository.findById(UUID.fromString(postId)).orElseThrow().getReactionCount()).isZero();
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
