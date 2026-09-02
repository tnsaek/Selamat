package com.tinsae.socialmediaplatform.reaction.service;

import com.tinsae.socialmediaplatform.common.enums.NotificationType;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.mapper.ReactionMapper;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.reaction.dto.ReactionRequest;
import com.tinsae.socialmediaplatform.reaction.dto.ReactionResponse;
import com.tinsae.socialmediaplatform.reaction.entity.Reaction;
import com.tinsae.socialmediaplatform.reaction.repository.ReactionRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final UserService userService;

    public ReactionService(
            ReactionRepository reactionRepository,
            NotificationService notificationService,
            PostRepository postRepository,
            UserService userService
    ) {
        this.reactionRepository = reactionRepository;
        this.notificationService = notificationService;
        this.postRepository = postRepository;
        this.userService = userService;
    }

    @Transactional
    public ReactionResponse reactToPost(UUID postId, ReactionRequest request, Jwt jwt) {
        User user = userService.getAuthenticatedUser(jwt);
        Post post = findVisiblePost(postId);

        Reaction reaction = reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())
                .orElseGet(() -> {
                    Reaction newReaction = new Reaction();
                    newReaction.setUser(user);
                    newReaction.setPost(post);
                    return newReaction;
                });

        boolean isNewReaction = reaction.getId() == null;
        reaction.setReactionType(request.reactionType());

        Reaction savedReaction = reactionRepository.save(reaction);
        if (isNewReaction) {
            post.setReactionCount(post.getReactionCount() + 1);
            postRepository.save(post);
            notificationService.createNotification(
                    post.getAuthor(),
                    user,
                    NotificationType.REACTION,
                    "New reaction",
                    user.getUsername() + " reacted to your post.",
                    "POST",
                    post.getId()
            );
        }

        return ReactionMapper.toResponse(savedReaction);
    }

    @Transactional
    public void removePostReaction(UUID postId, Jwt jwt) {
        User user = userService.getAuthenticatedUser(jwt);
        Post post = findVisiblePost(postId);

        Reaction reaction = reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reaction not found."));

        reactionRepository.delete(reaction);
        post.setReactionCount(Math.max(0, post.getReactionCount() - 1));
        postRepository.save(post);
    }

    private Post findVisiblePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found."));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Post not found.");
        }

        return post;
    }
}
