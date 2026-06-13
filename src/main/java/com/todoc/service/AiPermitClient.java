package com.todoc.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.todoc.dto.response.FormTemplateDetailResponse;
import com.todoc.dto.response.LandInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiPermitClient {

    private final RestClient aiRestClient;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PermitChatRequest(
            String message,
            String thread_id,
            @JsonProperty("land_context") Object landContext
    ) {}

    public record ChangeItem(
            Long questionId,
            String layoutKey,
            String name,
            Object previous,
            Object current,
            String source
    ) {}

    public record PermitChatResponse(String thread_id, String reply, String permit_type, List<ChangeItem> changes) {}

    public record CreateDocumentRequest(String thread_id, LandInfoResponse land_info, FormTemplateDetailResponse template) {}

    public record AnswerItem(Long question_id, String answer_value) {}

    public record FilledQuestion(Long id, String answer) {}

    public record FilledSection(Long id, List<FilledQuestion> questions) {}

    public record CreateDocumentResponse(String thread_id, String templateCode,
            List<FilledSection> sections, int filled_count, int total_count) {
        public List<AnswerItem> toAnswerItems() {
            if (sections == null) return List.of();
            return sections.stream()
                    .flatMap(s -> s.questions().stream())
                    .filter(q -> q.answer() != null && !q.answer().isBlank())
                    .map(q -> new AnswerItem(q.id(), toJsonValue(q.answer())))
                    .toList();
        }

        private static String toJsonValue(String value) {
            String trimmed = value.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
                return trimmed;
            }
            return "\"" + trimmed.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    public PermitChatResponse chat(String message, Long sessionId, Object landContext) {
        try {
            log.info("[AI 요청] sessionId={}, message={}, hasLandContext={}", sessionId, message, landContext != null);
            PermitChatResponse response = aiRestClient.post()
                    .uri("/api/v1/permit/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new PermitChatRequest(message, sessionId.toString(), landContext))
                    .retrieve()
                    .body(PermitChatResponse.class);
            log.info("[AI 응답] sessionId={}, response={}", sessionId, response);
            return response;
        } catch (Exception e) {
            log.error("AI 백엔드 호출 실패: sessionId={}, error={}", sessionId, e.getMessage(), e);
            return new PermitChatResponse(sessionId.toString(), "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해주세요.", null, null);
        }
    }

    public CreateDocumentResponse createDocument(Long sessionId, LandInfoResponse landInfo, FormTemplateDetailResponse template) {
        try {
            return aiRestClient.post()
                    .uri("/api/v1/permit/document")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new CreateDocumentRequest(sessionId.toString(), landInfo, template))
                    .retrieve()
                    .body(CreateDocumentResponse.class);
        } catch (Exception e) {
            log.error("AI 문서 생성 실패: sessionId={}, error={}", sessionId, e.getMessage());
            return null;
        }
    }
}
