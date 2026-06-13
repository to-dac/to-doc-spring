package com.todoc.service;

import com.todoc.domain.ChatMessage;
import com.todoc.domain.ChatSession;
import com.todoc.domain.User;
import com.todoc.domain.enums.MessageRole;
import com.todoc.domain.enums.MessageStatus;
import com.todoc.dto.request.CreateChatSessionRequest;
import com.todoc.dto.request.SendChatMessageRequest;
import com.todoc.dto.response.ChatMessageResponse;
import com.todoc.dto.response.ChatSessionResponse;
import com.todoc.exception.NotFoundException;
import com.todoc.repository.ChatMessageRepository;
import com.todoc.repository.ChatSessionRepository;
import com.todoc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatSessionResponse createSession(CreateChatSessionRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다: id=" + request.userId()));

        ChatSession session = ChatSession.builder()
                .user(user)
                .address(request.address())
                .build();

        return ChatSessionResponse.from(chatSessionRepository.save(session));
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

    public SseEmitter sendStreamingMessage(Long sessionId, SendChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다: id=" + sessionId));

        // 1. 사용자 메시지 DB에 즉시 저장
        saveMessage(session, MessageRole.USER, request.content());

        // 2. SseEmitter 생성 (타임아웃 3분)
        SseEmitter emitter = new SseEmitter(180000L);

        // 첫 연결 상태 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("연결 성공. AI 답변을 준비 중입니다."));
        } catch (IOException e) {
            log.error("SSE connection event error", e);
            emitter.completeWithError(e);
            return emitter;
        }

        // 3. Mock AI Response 생성 및 청크 분할
        String fullResponse = getMockResponse(request.content());
        // 줄바꿈이나 마침표 뒤 공백을 기준으로 쪼갬
        String[] chunks = fullResponse.split("(?<=\\n)|(?<=\\.\\s)");

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        StringBuilder responseAccumulator = new StringBuilder();

        executor.scheduleAtFixedRate(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                try {
                    if (index < chunks.length) {
                        String chunk = chunks[index];
                        responseAccumulator.append(chunk);

                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(chunk));
                        index++;
                    } else {
                        // 모든 청크 전송 완료
                        // AI 답변 DB 저장
                        saveMessage(session, MessageRole.ASSISTANT, responseAccumulator.toString());

                        // done 이벤트 전송
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data("답변 종료"));

                        emitter.complete();
                        executor.shutdown();
                    }
                } catch (Exception e) {
                    log.error("Error during streaming SSE response", e);
                    emitter.completeWithError(e);
                    executor.shutdown();
                }
            }
        }, 500, 400, TimeUnit.MILLISECONDS);

        // 스트리밍 강제 종료나 완료 시 리소스 정리
        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(executor::shutdown);
        emitter.onError(e -> executor.shutdown());

        return emitter;
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

    private String getMockResponse(String prompt) {
        if (prompt.contains("머리") || prompt.contains("두통") || prompt.contains("뇌")) {
            return "머리가 아프시군요. 두통은 다양한 원인에 의해 발생할 수 있습니다.\n\n" +
                    "1. 일시적인 스트레스나 피로, 혹은 수분 부족으로 인한 긴장성 두통일 수 있습니다. 이 경우 충분한 물을 섭취하고 안정을 취하는 것이 도움이 됩니다.\n" +
                    "2. 한쪽 머리가 지끈거리고 속이 울렁거린다면 편두통일 가능성이 높습니다. 어두운 곳에서 휴식을 취하세요.\n" +
                    "3. 만약 갑작스럽고 태어나서 처음 겪어보는 극심한 두통이 발생하거나 시야 장애, 언어 장애가 동반된다면 즉시 응급실로 가셔야 합니다.\n\n" +
                    "현재 통증의 정도가 심하시다면 진통제를 드시고 편안하게 누워 휴식을 취해보시는 것을 추천드립니다.";
        } else if (prompt.contains("배") || prompt.contains("복통") || prompt.contains("소화") || prompt.contains("위")) {
            return "배에 통증이 있으시군요. 아픈 부위와 증상에 따라 의심해볼 수 있는 원인이 다릅니다.\n\n" +
                    "1. 명치 부근의 타는 듯한 통증은 위염이나 역류성 식도염의 신호일 수 있습니다.\n" +
                    "2. 배 전체가 쥐어짜듯 아프며 설사나 구토를 동반한다면 급성 장염일 가능성이 큽니다.\n" +
                    "3. 오른쪽 아랫배에 누르면 아픈 심한 압통이 느껴진다면 급성 충수염(맹장염)일 수 있으니 신속히 내원하셔야 합니다.\n\n" +
                    "당분간은 자극적인 음식을 피하시고 미지근한 물을 자주 섭취해 주세요. 통증이 가라앉지 않으면 꼭 진료를 받아보시길 바랍니다.";
        } else if (prompt.contains("가슴") || prompt.contains("심장") || prompt.contains("숨")) {
            return "가슴 부위의 통증이나 답답함은 신속한 주의가 필요한 경우가 많습니다.\n\n" +
                    "1. 쥐어짜는 듯한 통증이 가슴 중앙에서 시작되어 어깨나 턱으로 뻗치거나, 식은땀과 호흡 곤란이 동반된다면 협심증이나 심근경색 같은 심장 질환의 응급 신호일 수 있습니다. 즉시 119에 연락하세요.\n" +
                    "2. 숨을 깊게 들이마실 때 바늘로 찌르는 듯 아프다면 폐나 갈비뼈 근처 근육의 문제일 수 있습니다.\n" +
                    "3. 신물이 올라오며 타는 듯한 가슴 통증은 역류성 식도염일 수 있습니다.\n\n" +
                    "자가 진단보다는 내과나 순환기내과를 방문하셔서 정확한 검진을 받아보시는 것이 안전합니다.";
        } else {
            return "안녕하세요! 의사 상담 가이드 AI 도우미 ToDoc입니다.\n\n" +
                    "환자분의 불편하신 증상(예: 두통, 복통, 가슴 통증, 감기 등)에 대해 자세히 적어주시면, 예상되는 원인과 대처 요령을 안내해 드릴 수 있습니다.\n" +
                    "현재 어디가 어떻게 불편하신가요?";
        }
    }
}
