package com.tinsae.socialmediaplatform.message.dto;

import com.tinsae.socialmediaplatform.common.enums.MessageStatus;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UserSummaryResponse sender,
        UserSummaryResponse recipient,
        String content,
        MessageStatus status,
        Instant sentAt,
        Instant deliveredAt,
        Instant readAt
) {
}
