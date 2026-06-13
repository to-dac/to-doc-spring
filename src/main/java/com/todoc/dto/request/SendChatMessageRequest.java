package com.todoc.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SendChatMessageRequest(
        @NotBlank(message = "메시지 내용은 비어있을 수 없습니다.")
        String content
) {}
