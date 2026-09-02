package com.tinsae.socialmediaplatform.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull(message = "Recipient is required.")
        UUID recipientId,

        @NotBlank(message = "Message content is required.")
        @Size(max = 5000, message = "Message content must not exceed 5000 characters.")
        String content
) {
}
