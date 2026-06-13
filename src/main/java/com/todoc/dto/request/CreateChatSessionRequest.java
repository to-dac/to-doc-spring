package com.todoc.dto.request;

import com.todoc.dto.response.LandInfoResponse;
import jakarta.validation.constraints.NotNull;

public record CreateChatSessionRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        LandInfoResponse landInfo,
        Long templateId
) {}
