package com.tinsae.socialmediaplatform.admin.service;

import com.tinsae.socialmediaplatform.admin.entity.ModerationAuditLog;
import com.tinsae.socialmediaplatform.admin.repository.ModerationAuditLogRepository;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.message.entity.Message;
import com.tinsae.socialmediaplatform.message.repository.MessageRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.repository.UserRepository;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ModerationService {

    private final CommentRepository commentRepository;
    private final MediaRepository mediaRepository;
    private final MessageRepository messageRepository;
    private final ModerationAuditLogRepository moderationAuditLogRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public ModerationService(
            CommentRepository commentRepository,
            MediaRepository mediaRepository,
            MessageRepository messageRepository,
            ModerationAuditLogRepository moderationAuditLogRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            UserService userService
    ) {
        this.commentRepository = commentRepository;
        this.mediaRepository = mediaRepository;
        this.messageRepository = messageRepository;
        this.moderationAuditLogRepository = moderationAuditLogRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public void hidePost(UUID postId, Jwt jwt) {
        User moderator = userService.getAuthenticatedUser(jwt);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

        if (post.getStatus() == PostStatus.DELETED) {
            throw new BusinessRuleException("Deleted posts cannot be hidden.");
        }

        post.setStatus(PostStatus.HIDDEN);
        postRepository.save(post);
        audit(moderator, "HIDE_POST", ReportTargetType.POST, post.getId());
    }

    @Transactional
    public void hideComment(UUID commentId, Jwt jwt) {
        User moderator = userService.getAuthenticatedUser(jwt);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new BusinessRuleException("Deleted comments cannot be hidden.");
        }

        comment.setStatus(CommentStatus.HIDDEN);
        commentRepository.save(comment);
        audit(moderator, "HIDE_COMMENT", ReportTargetType.COMMENT, comment.getId());
    }

    @Transactional
    public void deleteMessage(UUID messageId, Jwt jwt) {
        User moderator = userService.getAuthenticatedUser(jwt);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));

        message.setStatus(MessageStatus.DELETED);
        messageRepository.save(message);
        audit(moderator, "DELETE_MESSAGE", ReportTargetType.MESSAGE, message.getId());
    }

    @Transactional
    public void hideMedia(UUID mediaId, Jwt jwt) {
        User moderator = userService.getAuthenticatedUser(jwt);
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found."));

        if (media.getStatus() == MediaStatus.DELETED) {
            throw new BusinessRuleException("Deleted media cannot be hidden.");
        }

        media.setStatus(MediaStatus.HIDDEN);
        mediaRepository.save(media);
        audit(moderator, "HIDE_MEDIA", ReportTargetType.MEDIA, media.getId());
    }

    @Transactional
    public void suspendUser(UUID userId, Jwt jwt) {
        User moderator = userService.getAuthenticatedUser(jwt);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (moderator.getId().equals(user.getId())) {
            throw new BusinessRuleException("You cannot suspend your own account.");
        }

        if (hasRole(user, "ADMIN")) {
            throw new BusinessRuleException("Admin accounts cannot be suspended from this action.");
        }

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessRuleException("Deleted users cannot be suspended.");
        }

        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        audit(moderator, "SUSPEND_USER", ReportTargetType.USER, user.getId());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles()
                .stream()
                .anyMatch(userRole -> roleName.equals(userRole.getRole().getName()));
    }

    private void audit(User actor, String action, ReportTargetType targetType, UUID targetId) {
        ModerationAuditLog auditLog = new ModerationAuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        moderationAuditLogRepository.save(auditLog);
    }
}
