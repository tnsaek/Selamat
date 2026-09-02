package com.tinsae.socialmediaplatform.message.controller;

import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.message.dto.MessageResponse;
import com.tinsae.socialmediaplatform.message.dto.SendMessageRequest;
import com.tinsae.socialmediaplatform.message.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public PageResponse<MessageResponse> listMessages(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return messageService.listMessages(cursor, limit, jwt);
    }

    @GetMapping("/{messageId}")
    public MessageResponse getMessageById(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return messageService.getMessageById(messageId, jwt);
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(request, jwt));
    }

    @PatchMapping("/conversations/{participantId}/read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable UUID participantId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        messageService.markConversationAsRead(participantId, jwt);
        return ResponseEntity.noContent().build();
    }
}
