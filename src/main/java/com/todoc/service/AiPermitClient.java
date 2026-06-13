package com.todoc.service;

import com.todoc.dto.response.FormTemplateDetailResponse;
import com.todoc.dto.response.LandInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiPermitClient {

    private final RestClient aiRestClient;

    public record PermitChatRequest(String message, String thread_id) {}

    public record PermitChatResponse(String thread_id, String reply, String permit_type) {}

    public record CreateDocumentRequest(String thread_id, LandInfoResponse land_info, FormTemplateDetailResponse template) {}

    public record CreateDocumentResponse(String document_id, String status) {}

    public PermitChatResponse chat(String message, Long sessionId) {
        try {
            return aiRestClient.post()
                    .uri("/api/v1/permit/chat")
                    .body(new PermitChatRequest(message, sessionId.toString()))
                    .retrieve()
                    .body(PermitChatResponse.class);
        } catch (Exception e) {
            log.error("AI 백엔드 호출 실패: {}", e.getMessage());
            return new PermitChatResponse(sessionId.toString(), "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해주세요.", null);
        }
    }

    public CreateDocumentResponse createDocument(Long sessionId, LandInfoResponse landInfo, FormTemplateDetailResponse template) {
        try {
            return aiRestClient.post()
                    .uri("/api/v1/documents")
                    .body(new CreateDocumentRequest(sessionId.toString(), landInfo, template))
                    .retrieve()
                    .body(CreateDocumentResponse.class);
        } catch (Exception e) {
            log.error("AI 문서 생성 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }
}
