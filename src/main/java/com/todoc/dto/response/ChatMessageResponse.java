package com.todoc.dto.response;

import com.todoc.domain.ChatMessage;
import com.todoc.domain.enums.MessageRole;
import com.todoc.domain.enums.MessageStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageResponse(
        Long id,
        MessageRole role,
        String content,
        MessageStatus status,
        LocalDateTime createdAt,
        List<AttachmentSummary> attachments
) {
    public record AttachmentSummary(Long id, String name) {}

    public static ChatMessageResponse from(ChatMessage message) {
        List<AttachmentSummary> summaries = message.getAttachments().stream()
                .map(a -> new AttachmentSummary(a.getId(), a.getName()))
                .toList();

        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt(),
                summaries
        );
    }
}
