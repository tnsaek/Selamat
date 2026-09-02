package com.tinsae.socialmediaplatform.reaction.dto;

import com.tinsae.socialmediaplatform.common.enums.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(
        @NotNull(message = "Reaction type is required.")
        ReactionType reactionType
) {
}
