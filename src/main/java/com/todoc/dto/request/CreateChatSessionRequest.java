package com.todoc.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateChatSessionRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,
        
        String address
) {}
