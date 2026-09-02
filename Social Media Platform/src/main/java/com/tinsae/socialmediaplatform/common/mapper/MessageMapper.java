package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.message.dto.MessageResponse;
import com.tinsae.socialmediaplatform.message.entity.Message;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                UserMapper.toSummary(message.getSender()),
                UserMapper.toSummary(message.getRecipient()),
                message.getContent(),
                message.getStatus(),
                message.getSentAt(),
                message.getDeliveredAt(),
                message.getReadAt()
        );
    }
}
