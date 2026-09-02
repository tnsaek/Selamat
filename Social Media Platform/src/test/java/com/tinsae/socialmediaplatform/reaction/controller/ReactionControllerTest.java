package com.tinsae.socialmediaplatform.reaction.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import com.tinsae.socialmediaplatform.reaction.dto.ReactionRequest;
import com.tinsae.socialmediaplatform.reaction.dto.ReactionResponse;
import com.tinsae.socialmediaplatform.reaction.service.ReactionService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionControllerTest {

    @Mock
    private ReactionService reactionService;

    @InjectMocks
    private ReactionController reactionController;

    @Test
    void reactToPostDelegatesToReactionService() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        UUID postId = UUID.randomUUID();
        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);
        ReactionResponse reaction = reactionResponse(user.getId(), postId);
        when(reactionService.reactToPost(postId, request, jwt)).thenReturn(reaction);

        ReactionResponse response = reactionController.reactToPost(postId, request, jwt);

        assertThat(response).isSameAs(reaction);
        verify(reactionService).reactToPost(postId, request, jwt);
    }

    @Test
    void removePostReactionReturnsNoContent() {
        User user = TestDataFactory.user("user");
        var jwt = TestDataFactory.jwt(user);
        UUID postId = UUID.randomUUID();

        var response = reactionController.removePostReaction(postId, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(reactionService).removePostReaction(postId, jwt);
    }

    private ReactionResponse reactionResponse(UUID userId, UUID postId) {
        return new ReactionResponse(
                UUID.randomUUID(),
                userId,
                postId,
                null,
                ReactionType.LIKE,
                Instant.now()
        );
    }
}
