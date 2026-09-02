package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.reaction.dto.ReactionResponse;
import com.tinsae.socialmediaplatform.reaction.entity.Reaction;

public final class ReactionMapper {

    private ReactionMapper() {
    }

    public static ReactionResponse toResponse(Reaction reaction) {
        return new ReactionResponse(
                reaction.getId(),
                reaction.getUser().getId(),
                reaction.getPost() != null ? reaction.getPost().getId() : null,
                reaction.getComment() != null ? reaction.getComment().getId() : null,
                reaction.getReactionType(),
                reaction.getCreatedAt()
        );
    }
}
