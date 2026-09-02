package com.tinsae.socialmediaplatform;

import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.FollowStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.PostVisibility;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.follow.entity.Follow;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.notification.entity.Notification;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.profile.entity.Profile;
import com.tinsae.socialmediaplatform.report.entity.Report;
import com.tinsae.socialmediaplatform.role.entity.Role;
import com.tinsae.socialmediaplatform.role.entity.UserRole;
import com.tinsae.socialmediaplatform.role.entity.UserRoleId;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());

        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        profile.setDisplayName(username);
        user.setProfile(profile);

        return user;
    }

    public static Role role(String name) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(name);
        return role;
    }

    public static UserRole userRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setId(new UserRoleId(user.getId(), role.getId()));
        userRole.setUser(user);
        userRole.setRole(role);
        user.getRoles().add(userRole);
        return userRole;
    }

    public static Post post(User author) {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setAuthor(author);
        post.setContent("Post content");
        post.setVisibility(PostVisibility.PUBLIC);
        post.setStatus(PostStatus.PUBLISHED);
        post.setCommentCount(0L);
        post.setReactionCount(0L);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        return post;
    }

    public static Comment comment(Post post, User author) {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID());
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent("Comment content");
        comment.setStatus(CommentStatus.VISIBLE);
        comment.setReactionCount(0L);
        comment.setCreatedAt(Instant.now());
        return comment;
    }

    public static Follow follow(User follower, User followed) {
        Follow follow = new Follow();
        follow.setId(UUID.randomUUID());
        follow.setFollower(follower);
        follow.setFollowed(followed);
        follow.setStatus(FollowStatus.ACCEPTED);
        follow.setCreatedAt(Instant.now());
        return follow;
    }

    public static Message message(User sender, User recipient) {
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent("Hello");
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());
        return message;
    }

    public static Media media(User uploader) {
        Media media = new Media();
        media.setId(UUID.randomUUID());
        media.setUploader(uploader);
        media.setUrl("https://example.com/media.png");
        media.setMediaType(MediaType.IMAGE);
        media.setMimeType("image/png");
        media.setSizeBytes(2048L);
        media.setAltText("Media");
        media.setStatus(MediaStatus.VISIBLE);
        media.setCreatedAt(Instant.now());
        return media;
    }

    public static Notification notification(User recipient, User actor) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setNotificationType(NotificationType.MESSAGE);
        notification.setTitle("New message");
        notification.setBody("You have a message");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        return notification;
    }

    public static Report report(User reporter, UUID targetId) {
        Report report = new Report();
        report.setId(UUID.randomUUID());
        report.setReporter(reporter);
        report.setTargetType(ReportTargetType.POST);
        report.setTargetId(targetId);
        report.setReason("Spam");
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(Instant.now());
        return report;
    }

    public static Jwt jwt(User user) {
        Instant now = Instant.now();
        return new Jwt(
                "token",
                now,
                now.plusSeconds(900),
                Map.of("alg", "HS256"),
                Map.of("sub", user.getId().toString())
        );
    }
}
