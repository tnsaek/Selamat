package com.tinsae.socialmediaplatform.comment.service;

import com.tinsae.socialmediaplatform.comment.dto.CommentResponse;
import com.tinsae.socialmediaplatform.comment.dto.CreateCommentRequest;
import com.tinsae.socialmediaplatform.comment.entity.Comment;
import com.tinsae.socialmediaplatform.comment.repository.CommentRepository;
import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.CommentStatus;
import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.mapper.CommentMapper;
import com.tinsae.socialmediaplatform.common.util.CursorUtils;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
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
public class CommentService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final UserService userService;

    public CommentService(
            CommentRepository commentRepository,
            NotificationService notificationService,
            PostRepository postRepository,
            UserService userService
    ) {
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.postRepository = postRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listPostComments(UUID postId, String cursor, Integer limit) {
        Post post = findVisiblePost(postId);
        int pageSize = normalizeLimit(limit);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Instant cursorInstant = CursorUtils.parseCursor(cursor);

        List<Comment> visibleComments = new ArrayList<>(commentRepository
                .findVisiblePostComments(post.getId(), CommentStatus.VISIBLE, cursorInstant, pageable));
        String nextCursor = CursorUtils.trimAndNextCursor(visibleComments, pageSize, Comment::getCreatedAt);

        List<CommentResponse> comments = visibleComments
                .stream()
                .map(CommentMapper::toResponse)
                .toList();

        return new PageResponse<>(comments, nextCursor);
    }

    @Transactional
    public CommentResponse createComment(UUID postId, CreateCommentRequest request, Jwt jwt) {
        User author = userService.getAuthenticatedUser(jwt);
        Post post = findVisiblePost(postId);

        Comment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = findVisibleComment(request.parentCommentId());
            if (!parentComment.getPost().getId().equals(post.getId())) {
                throw new BusinessRuleException("Parent comment must belong to the same post.");
            }
        }

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setParentComment(parentComment);
        comment.setContent(request.content());
        comment.setStatus(CommentStatus.VISIBLE);

        Comment savedComment = commentRepository.save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        notificationService.createNotification(
                post.getAuthor(),
                author,
                NotificationType.COMMENT,
                "New comment",
                author.getUsername() + " commented on your post.",
                "POST",
                post.getId()
        );

        return CommentMapper.toResponse(savedComment);
    }

    private Comment findVisibleComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));

        if (comment.getStatus() != CommentStatus.VISIBLE) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        return comment;
    }

    private Post findVisiblePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Post not found.");
        }

        return post;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
