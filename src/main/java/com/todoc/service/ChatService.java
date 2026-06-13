package com.todoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todoc.domain.ChatMessage;
import com.todoc.domain.ChatSession;
import com.todoc.domain.User;
import com.todoc.domain.enums.MessageRole;
import com.todoc.domain.enums.MessageStatus;
import com.todoc.dto.request.CreateChatSessionRequest;
import com.todoc.dto.request.SendChatMessageRequest;
import com.todoc.dto.response.ChatMessageResponse;
import com.todoc.dto.response.ChatSessionResponse;
import com.todoc.domain.FormSubmission;
import com.todoc.dto.response.FormSubmissionResponse;
import com.todoc.dto.response.LandInfoResponse;
import com.todoc.exception.NotFoundException;
import com.todoc.repository.ChatMessageRepository;
import com.todoc.repository.ChatSessionRepository;
import com.todoc.repository.FormTemplateRepository;
import com.todoc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Map<String, String> PERMIT_TYPE_TO_TEMPLATE_CODE = Map.of(
            "building", "building_major_repair_use_change_permit",
            "dev_act",  "development_activity_permit",
            "farmland", "farmland_conversion_permit",
            "road",     "road_permit",
            "river",    "river_permit",
            "mountain", "mountain_permit"
    );

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final AiPermitClient aiPermitClient;
    private final FormTemplateService formTemplateService;
    private final FormSubmissionService formSubmissionService;
    private final DocumentProcessingService documentProcessingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatSessionResponse createSession(CreateChatSessionRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: id=" + request.userId()));

        LandInfoResponse landInfo = request.landInfo();
        String address = landInfo != null ? landInfo.address() : null;
        String pnu = landInfo != null ? landInfo.pnu() : null;

        String title = "새 채팅";
        if (address != null && !address.trim().isEmpty()) {
            title = address.split("\\|")[0];
        }

        ChatSession session = ChatSession.builder()
                .user(user)
                .title(title)
                .address(address)
                .pnu(pnu)
                .templateId(request.templateId())
                .build();

        if (landInfo != null) {
            try {
                session.updateLandInfo(objectMapper.writeValueAsString(landInfo));
            } catch (JsonProcessingException e) {
                log.error("토지 정보 직렬화 실패: sessionId={}", session.getId(), e);
            }
        }

        chatSessionRepository.save(session);
        return ChatSessionResponse.from(session);
    }

    @Transactional
    public FormSubmissionResponse generateDocument(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));

        if (session.getTemplateId() == null) {
            throw new IllegalStateException("세션에 템플릿이 설정되지 않았습니다.");
        }
        if (session.getLandInfo() == null) {
            throw new IllegalStateException("세션에 토지 정보가 없습니다.");
        }

        FormSubmission sub = formSubmissionService.createPending(
                session.getTemplateId(), session.getUser().getId(), session);
        session.updateSubmissionId(sub.getId());

        documentProcessingService.process(sessionId, sub.getId());

        return formSubmissionService.getById(sub.getId());
    }

    @Transactional(readOnly = true)
    public FormSubmissionResponse getDocument(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));
        if (session.getSubmissionId() == null) {
            throw new NotFoundException("생성된 문서가 없습니다. POST /sessions/" + sessionId + "/document 를 먼저 호출하세요.");
        }
        return formSubmissionService.getById(session.getSubmissionId());
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessionsByUserId(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesBySessionId(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long sessionId, SendChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));

        Object landContext = resolveLandContext(sessionId, session);
        saveMessage(session, MessageRole.USER, request.content());

        AiPermitClient.PermitChatResponse aiResponse = aiPermitClient.chat(request.content(), sessionId, landContext);

        if (aiResponse.permit_type() != null) {
            String templateCode = PERMIT_TYPE_TO_TEMPLATE_CODE.get(aiResponse.permit_type());
            if (templateCode != null) {
                formTemplateRepository.findByTemplateCode(templateCode)
                        .ifPresent(t -> session.updateTemplateId(t.getId()));
            }
        }

        if (aiResponse.changes() != null && !aiResponse.changes().isEmpty() && session.getSubmissionId() != null) {
            formSubmissionService.applyChanges(session.getSubmissionId(), aiResponse.changes());
        }

        ChatMessage assistantMessage = ChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(aiResponse.reply())
                .status(MessageStatus.COMPLETED)
                .build();

        return ChatMessageResponse.from(chatMessageRepository.save(assistantMessage));
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.delete(session);
    }

    public SseEmitter sendStreamingMessage(Long sessionId, SendChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));

        Object landContext = resolveLandContext(sessionId, session);
        saveMessage(session, MessageRole.USER, request.content());

        SseEmitter emitter = new SseEmitter(180000L);

        try {
            emitter.send(SseEmitter.event().name("connect").data("연결 성공. AI 답변을 준비 중입니다."));
        } catch (IOException e) {
            log.error("SSE connection event error", e);
            emitter.completeWithError(e);
            return emitter;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                AiPermitClient.PermitChatResponse aiResponse = aiPermitClient.chat(request.content(), sessionId, landContext);
                String fullResponse = aiResponse.reply();

                if (aiResponse.permit_type() != null) {
                    String templateCode = PERMIT_TYPE_TO_TEMPLATE_CODE.get(aiResponse.permit_type());
                    if (templateCode != null) {
                        formTemplateRepository.findByTemplateCode(templateCode).ifPresent(template ->
                            chatSessionRepository.findById(sessionId).ifPresent(s -> {
                                s.updateTemplateId(template.getId());
                                chatSessionRepository.save(s);
                            })
                        );
                    }
                }

                boolean changesApplied = false;
                if (aiResponse.changes() != null && !aiResponse.changes().isEmpty()) {
                    changesApplied = chatSessionRepository.findById(sessionId)
                            .map(s -> {
                                if (s.getSubmissionId() != null) {
                                    formSubmissionService.applyChanges(s.getSubmissionId(), aiResponse.changes());
                                    return true;
                                }
                                return false;
                            }).orElse(false);
                }

                String[] chunks = fullResponse.split("(?<=\\n)|(?<=\\.\\s)");
                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    Thread.sleep(80);
                }

                if (changesApplied) {
                    emitter.send(SseEmitter.event().name("changes")
                            .data(objectMapper.writeValueAsString(aiResponse.changes())));
                }

                saveMessage(session, MessageRole.ASSISTANT, fullResponse);
                emitter.send(SseEmitter.event().name("done").data("답변 종료"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Error during streaming SSE response", e);
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(executor::shutdown);
        emitter.onError(e -> executor.shutdown());

        return emitter;
    }

    private Object resolveLandContext(Long sessionId, ChatSession session) {
        boolean isFirstUserMessage = chatMessageRepository.countBySessionIdAndRole(sessionId, MessageRole.USER) == 0;
        if (!isFirstUserMessage || session.getLandInfo() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(session.getLandInfo(), Object.class);
        } catch (Exception e) {
            log.warn("land_info 파싱 실패: sessionId={}", sessionId);
            return null;
        }
    }

    @Transactional
    public void saveMessage(ChatSession session, MessageRole role, String content) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .status(MessageStatus.COMPLETED)
                .build();
        chatMessageRepository.save(message);
    }
}
