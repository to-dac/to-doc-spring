package com.todoc.dto.response;

import com.todoc.domain.ChatSession;
import java.time.LocalDateTime;

public record ChatSessionResponse(
        Long id,
        Long userId,
        String title,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(),
                session.getUser().getId(),
                session.getTitle(),
                session.getAddress(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
