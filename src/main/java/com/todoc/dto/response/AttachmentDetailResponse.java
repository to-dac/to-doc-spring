package com.todoc.dto.response;

import com.todoc.domain.Attachment;

import java.time.LocalDateTime;

public record AttachmentDetailResponse(
        Long id,
        String name,
        String url,
        LocalDateTime createdAt
) {
    public static AttachmentDetailResponse from(Attachment attachment) {
        return new AttachmentDetailResponse(
                attachment.getId(),
                attachment.getName(),
                attachment.getUrl(),
                attachment.getCreatedAt()
        );
    }
}
