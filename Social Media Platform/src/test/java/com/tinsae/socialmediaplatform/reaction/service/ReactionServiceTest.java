package com.tinsae.socialmediaplatform.reaction.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.PostStatus;
import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.notification.service.NotificationService;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.post.repository.PostRepository;
import com.tinsae.socialmediaplatform.reaction.dto.ReactionRequest;
import com.tinsae.socialmediaplatform.reaction.entity.Reaction;
import com.tinsae.socialmediaplatform.reaction.repository.ReactionRepository;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReactionService reactionService;

    @Test
    void reactToPostCreatesReactionAndIncrementsCount() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())).thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(invocation -> {
            Reaction reaction = invocation.getArgument(0);
            reaction.setId(UUID.randomUUID());
            return reaction;
        });

        var response = reactionService.reactToPost(post.getId(), new ReactionRequest(ReactionType.LIKE), jwt);

        assertThat(response.reactionType()).isEqualTo(ReactionType.LIKE);
        assertThat(post.getReactionCount()).isEqualTo(1L);
        verify(postRepository).save(post);
    }

    @Test
    void reactToPostUpdatesExistingReactionWithoutIncrementingCount() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        post.setReactionCount(5L);
        Reaction reaction = reaction(user, post, ReactionType.LIKE);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())).thenReturn(Optional.of(reaction));
        when(reactionRepository.save(reaction)).thenReturn(reaction);

        var response = reactionService.reactToPost(post.getId(), new ReactionRequest(ReactionType.LOVE), jwt);

        assertThat(response.reactionType()).isEqualTo(ReactionType.LOVE);
        assertThat(post.getReactionCount()).isEqualTo(5L);
    }

    @Test
    void reactToPostRejectsMissingPost() {
        User user = TestDataFactory.user("user");
        UUID postId = UUID.randomUUID();
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactionService.reactToPost(postId, new ReactionRequest(ReactionType.LIKE), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void reactToPostRejectsDeletedPost() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        post.setStatus(PostStatus.DELETED);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> reactionService.reactToPost(post.getId(), new ReactionRequest(ReactionType.LIKE), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void reactToPostRejectsHiddenPost() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        post.setStatus(PostStatus.HIDDEN);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> reactionService.reactToPost(post.getId(), new ReactionRequest(ReactionType.LIKE), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found.");
    }

    @Test
    void removePostReactionDeletesReactionAndDecrementsCount() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        post.setReactionCount(1L);
        Reaction reaction = new Reaction();
        reaction.setId(UUID.randomUUID());
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setReactionType(ReactionType.LIKE);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())).thenReturn(Optional.of(reaction));

        reactionService.removePostReaction(post.getId(), jwt);

        assertThat(post.getReactionCount()).isZero();
        verify(reactionRepository).delete(reaction);
        verify(postRepository).save(post);
    }

    @Test
    void removePostReactionDoesNotDecrementBelowZero() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        post.setReactionCount(0L);
        Reaction reaction = reaction(user, post, ReactionType.LIKE);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())).thenReturn(Optional.of(reaction));

        reactionService.removePostReaction(post.getId(), jwt);

        assertThat(post.getReactionCount()).isZero();
        verify(reactionRepository).delete(reaction);
    }

    @Test
    void removePostReactionRejectsMissingReaction() {
        User user = TestDataFactory.user("user");
        Post post = TestDataFactory.post(user);
        var jwt = TestDataFactory.jwt(user);
        when(userService.getAuthenticatedUser(jwt)).thenReturn(user);
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(reactionRepository.findByUserIdAndPostId(user.getId(), post.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactionService.removePostReaction(post.getId(), jwt))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Reaction not found.");
    }

    private Reaction reaction(User user, Post post, ReactionType reactionType) {
        Reaction reaction = new Reaction();
        reaction.setId(UUID.randomUUID());
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setReactionType(reactionType);
        return reaction;
    }
}
