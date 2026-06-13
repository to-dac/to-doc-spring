package com.todoc.dto.response;

import com.todoc.domain.ChatMessage;
import com.todoc.domain.enums.MessageRole;
import com.todoc.domain.enums.MessageStatus;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        MessageRole role,
        String content,
        MessageStatus status,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt()
        );
    }
}
